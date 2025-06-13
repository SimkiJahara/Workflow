package com.simki.workflowapp

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.simki.workflowapp.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WorkflowAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WorkflowApp()
                }
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class Workflow(
    val name: String,
    val actions: List<String> = emptyList()
)

object Spacing {
    val small: Dp = 8.dp
    val medium: Dp = 16.dp
}

@Composable
fun WorkflowApp() {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("WorkflowPrefs", Context.MODE_PRIVATE)
    val snackbarHostState = remember { SnackbarHostState() }

    var workflows by remember {
        mutableStateOf(loadWorkflows(sharedPreferences))
    }
    var workflowCounter by remember {
        mutableIntStateOf(sharedPreferences.getInt("workflowCounter", 1))
    }
    var selectedWorkflowIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(workflows) {
        saveWorkflows(sharedPreferences, workflows)
    }
    LaunchedEffect(workflowCounter) {
        sharedPreferences.edit {
            putInt("workflowCounter", workflowCounter)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        content = { padding ->
            Crossfade(targetState = selectedWorkflowIndex >= 0) { isEditorOpen ->
                if (isEditorOpen) {
                    DetailedWorkflowEditor(
                        workflow = workflows[selectedWorkflowIndex],
                        onBack = { selectedWorkflowIndex = -1 },
                        onRename = { newName ->
                            workflows = workflows.toMutableList().also {
                                it[selectedWorkflowIndex] = it[selectedWorkflowIndex].copy(name = newName)
                            }
                        },
                        onAddAction = { action ->
                            workflows = workflows.toMutableList().also {
                                it[selectedWorkflowIndex] = it[selectedWorkflowIndex].copy(
                                    actions = it[selectedWorkflowIndex].actions + action
                                )
                            }
                        },
                        onEditAction = { index, newAction ->
                            workflows = workflows.toMutableList().also {
                                val actions = it[selectedWorkflowIndex].actions.toMutableList()
                                actions[index] = newAction
                                it[selectedWorkflowIndex] = it[selectedWorkflowIndex].copy(actions = actions)
                            }
                        },
                        onDeleteAction = { index ->
                            workflows = workflows.toMutableList().also {
                                val actions = it[selectedWorkflowIndex].actions.toMutableList()
                                actions.removeAt(index)
                                it[selectedWorkflowIndex] = it[selectedWorkflowIndex].copy(actions = actions)
                            }
                        },
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    WorkflowHomeScreen(
                        workflows = workflows,
                        workflowCounter = workflowCounter,
                        onWorkflowsChanged = { workflows = it },
                        onWorkflowCounterChanged = { workflowCounter = it },
                        onWorkflowSelected = { selectedWorkflowIndex = it },
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    )
}

fun loadWorkflows(sharedPreferences: SharedPreferences): List<Workflow> {
    val json = sharedPreferences.getString("workflows", null)
    return if (json != null) {
        try {
            Json.decodeFromString<List<Workflow>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    } else {
        emptyList()
    }
}

fun saveWorkflows(sharedPreferences: SharedPreferences, workflows: List<Workflow>) {
    val json = Json.encodeToString(workflows)
    sharedPreferences.edit {
        putString("workflows", json)
    }
}

@Composable
fun WorkflowHomeScreen(
    workflows: List<Workflow>,
    workflowCounter: Int,
    onWorkflowsChanged: (List<Workflow>) -> Unit,
    onWorkflowCounterChanged: (Int) -> Unit,
    onWorkflowSelected: (Int) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var newWorkflowName by remember { mutableStateOf("") }
    var initialAction by remember { mutableStateOf("") }
    var showDeleteSnackbar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val themeState = LocalThemeState.current

    val filteredWorkflows = workflows.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    LaunchedEffect(showDeleteSnackbar) {
        if (showDeleteSnackbar) {
            snackbarHostState.showSnackbar("Workflow deleted")
            showDeleteSnackbar = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(Spacing.medium)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Workflows") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.medium),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Workflows",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(
                onClick = themeState.toggleTheme,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Brightness4,
                    contentDescription = "Toggle Theme",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.small, Alignment.Top)
        ) {
            itemsIndexed(filteredWorkflows) { index, workflow ->
                WorkflowCard(
                    workflow = workflow,
                    onEditClick = {
                        editingIndex = workflows.indexOf(workflow)
                        editText = workflow.name
                        showEditDialog = true
                    },
                    onSelectClick = { onWorkflowSelected(workflows.indexOf(workflow)) },
                    onDeleteClick = {
                        onWorkflowsChanged(workflows.toMutableList().also { it.removeAt(workflows.indexOf(workflow)) })
                        showDeleteSnackbar = true
                    }
                )
            }
        }

        FilledButton(
            onClick = {
                newWorkflowName = "Workflow #$workflowCounter"
                initialAction = ""
                showCreateDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.medium)
                .height(56.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Workflow", tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(Spacing.small))
                Text("Create New Workflow", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }

    if (showCreateDialog) {
        WorkflowDialog(
            title = "Create New Workflow",
            workflowName = newWorkflowName,
            initialAction = initialAction,
            onWorkflowNameChange = { newWorkflowName = it },
            onInitialActionChange = { initialAction = it },
            onConfirm = {
                val newWorkflow = Workflow(
                    name = newWorkflowName,
                    actions = if (initialAction.isNotBlank()) listOf(initialAction) else emptyList()
                )
                onWorkflowsChanged(workflows + newWorkflow)
                onWorkflowCounterChanged(workflowCounter + 1)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
            isConfirmEnabled = newWorkflowName.isNotBlank()
        )
    }

    if (showEditDialog) {
        WorkflowDialog(
            title = "Edit Workflow",
            workflowName = editText,
            onWorkflowNameChange = { editText = it },
            onConfirm = {
                onWorkflowsChanged(workflows.toMutableList().also {
                    it[editingIndex] = it[editingIndex].copy(name = editText)
                })
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false },
            isConfirmEnabled = editText.isNotBlank()
        )
    }
}

@Composable
fun WorkflowCard(
    workflow: Workflow,
    onEditClick: () -> Unit,
    onSelectClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 150)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = androidx.compose.material3.ripple()
            ) {
                isPressed = true
                onEditClick()
                isPressed = false
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = workflow.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = Spacing.medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row {
                ActionIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "View Details",
                    onClick = onSelectClick,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                ActionIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Delete Workflow",
                    onClick = onDeleteClick,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun ActionIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(40.dp)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}

@Composable
fun WorkflowDialog(
    title: String,
    workflowName: String,
    initialAction: String = "",
    onWorkflowNameChange: (String) -> Unit,
    onInitialActionChange: (String) -> Unit = {},
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isConfirmEnabled: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = workflowName,
                    onValueChange = onWorkflowNameChange,
                    singleLine = true,
                    label = { Text("Workflow Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                if (initialAction.isNotEmpty() || title == "Create New Workflow") {
                    Spacer(modifier = Modifier.height(Spacing.medium))
                    OutlinedTextField(
                        value = initialAction,
                        onValueChange = onInitialActionChange,
                        singleLine = true,
                        label = { Text("Initial Action (e.g., Send Email)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = isConfirmEnabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedWorkflowEditor(
    workflow: Workflow,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onAddAction: (String) -> Unit,
    onEditAction: (Int, String) -> Unit,
    onDeleteAction: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(workflow.name) }
    var newAction by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workflow Editor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        content = { padding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
                    .padding(Spacing.medium)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        onRename(it)
                    },
                    label = { Text("Workflow Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(Spacing.medium))

                Text(
                    text = "Actions",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = Spacing.small),
                    verticalArrangement = Arrangement.spacedBy(Spacing.small, Alignment.Top)
                ) {
                    itemsIndexed(workflow.actions) { index, action ->
                        ActionCard(
                            action = action,
                            index = index,
                            onEdit = onEditAction,
                            onDelete = onDeleteAction
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newAction,
                        onValueChange = { newAction = it },
                        label = { Text("Add Action") },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = Spacing.small),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.secondary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    FilledIconButton(
                        onClick = {
                            if (newAction.isNotBlank()) {
                                onAddAction(newAction)
                                newAction = ""
                            }
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Action",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                FilledButton(
                    onClick = {
                        workflow.actions.forEach { action ->
                            println("Executing action: $action")
                        }
                        CoroutineScope(Dispatchers.Main).launch {
                            snackbarHostState.showSnackbar("Workflow executed!")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.medium)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Run Workflow",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(Spacing.small))
                        Text("Run Workflow", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    )
}

@Composable
fun ActionCard(
    action: String,
    index: Int,
    onEdit: (Int, String) -> Unit,
    onDelete: (Int) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editedAction by remember { mutableStateOf(action) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditing) {
                OutlinedTextField(
                    value = editedAction,
                    onValueChange = { editedAction = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = Spacing.small),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                ActionIconButton(
                    icon = Icons.Default.Check,
                    contentDescription = "Save Action",
                    onClick = {
                        if (editedAction.isNotBlank()) {
                            onEdit(index, editedAction)
                            isEditing = false
                        }
                    },
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Action",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { isEditing = true }
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(
                    text = action,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                ActionIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = "Delete Action",
                    onClick = { onDelete(index) },
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun FilledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150)
    )

    Button(
        onClick = {
            isPressed = true
            onClick()
            isPressed = false
        },
        modifier = modifier
            .scale(scale)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        content = { Row(content = content) }
    )
}

@Composable
fun FilledIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150)
    )

    IconButton(
        onClick = {
            isPressed = true
            onClick()
            isPressed = false
        },
        modifier = modifier
            .scale(scale)
            .background(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(24.dp)
        ) {
            content()
        }
    }
}

