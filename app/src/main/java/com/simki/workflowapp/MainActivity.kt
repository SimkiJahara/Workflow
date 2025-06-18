package com.simki.workflowapp

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { firebaseAuthWithGoogle(it) } ?: run {
                Toast.makeText(this, "Google Sign-In failed: No ID token", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In failed: ${e.statusCode} - ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                setContent { AppContent() }
            } else {
                Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Request permissions
        requestPermissions()

        if (auth.currentUser != null) {
            setContent { AppContent() }
        } else {
            setContent { LoginScreen { signInLauncher.launch(googleSignInClient.signInIntent) } }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val launcher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            results.forEach { (permission, granted) ->
                if (!granted) {
                    Toast.makeText(this, "$permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
        launcher.launch(permissions.toTypedArray())
    }
}

@Composable
fun AppContent() {
    var isDarkTheme by remember { mutableStateOf(false) }
    var currentScreen by remember { mutableStateOf("home") }
    MaterialTheme(
        colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Crossfade(targetState = currentScreen) { screen ->
                when (screen) {
                    "home" -> WorkflowApp(
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onModelScreen = { currentScreen = "model" }
                    )
                    "model" -> ModelInvocationScreen(
                        onBack = { currentScreen = "home" }
                    )
                }
            }
        }
    }
}

@Composable
fun LoginScreen(onSignInClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome to Workflow App",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        FilledButton(
            onClick = onSignInClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = "Sign In", tint = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign in with Google", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Serializable
data class Category(
    val name: String,
    val color: String
)

@Serializable
data class Workflow(
    val name: String,
    val actions: List<String> = emptyList(),
    val category: Category? = null
)

object Spacing {
    val small: Dp = 8.dp
    val medium: Dp = 16.dp
}

@Composable
fun WorkflowApp(onToggleTheme: () -> Unit, onModelScreen: () -> Unit) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("WorkflowPrefs", Context.MODE_PRIVATE)
    val snackbarHostState = remember { SnackbarHostState() }

    var workflows by remember {
        mutableStateOf(loadWorkflows(sharedPreferences))
    }
    var categories by remember {
        mutableStateOf(loadCategories(sharedPreferences))
    }
    var workflowCounter by remember {
        mutableIntStateOf(sharedPreferences.getInt("workflowCounter", 1))
    }
    var selectedWorkflowIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(workflows) {
        saveWorkflows(sharedPreferences, workflows)
    }
    LaunchedEffect(categories) {
        saveCategories(sharedPreferences, categories)
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
                        onCategoryChange = { category ->
                            workflows = workflows.toMutableList().also {
                                it[selectedWorkflowIndex] = it[selectedWorkflowIndex].copy(category = category)
                            }
                        },
                        categories = categories,
                        onAddCategory = { newCategory ->
                            categories = categories + newCategory
                        },
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    WorkflowHomeScreen(
                        workflows = workflows,
                        categories = categories,
                        workflowCounter = workflowCounter,
                        onWorkflowsChanged = { workflows = it },
                        onCategoriesChanged = { categories = it },
                        onWorkflowCounterChanged = { workflowCounter = it },
                        onWorkflowSelected = { selectedWorkflowIndex = it },
                        onToggleTheme = onToggleTheme,
                        onModelScreen = onModelScreen,
                        snackbarHostState = snackbarHostState,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelInvocationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var inputText by remember { mutableStateOf("") }
    var imagePath by remember { mutableStateOf<String?>(null) }
    var outputText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    val imagePicker = ActivityResultContracts.GetContent()
    val imageLauncher = rememberLauncherForActivityResult(contract = imagePicker) { uri ->
        imagePath = uri?.toString()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Model Invocation") },
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
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Input Text") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                FilledButton(
                    onClick = { imageLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Select Image")
                }
                imagePath?.let {
                    Text("Selected Image: $it", style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledButton(
                        onClick = {
                            isProcessing = true
                            CoroutineScope(Dispatchers.IO).launch {
                                val result = invokeM1(inputText)
                                outputText = result
                                showNotification(context, "M1 Result", result)
                                isProcessing = false
                            }
                        },
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        enabled = !isProcessing
                    ) {
                        Text("Run M1")
                    }
                    FilledButton(
                        onClick = {
                            isProcessing = true
                            CoroutineScope(Dispatchers.IO).launch {
                                val result = invokeM2(inputText, imagePath?.let { File(it) })
                                outputText = result
                                showNotification(context, "M2 Result", result)
                                isProcessing = false
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing && imagePath != null
                    ) {
                        Text("Run M2")
                    }
                }
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                if (outputText.isNotEmpty()) {
                    Text(
                        text = "Output: $outputText",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    )
                }
            }
        }
    )
}

// Placeholder for M1 (Instruction-following LLM)
fun invokeM1(input: String): String {
    return "M1 Output: Processed '$input'"
}

// Placeholder for M2 (Multimodal LLM)
fun invokeM2(input: String, image: File?): String {
    return if (image != null) {
        "M2 Output: Processed '$input' with image ${image.name}"
    } else {
        "M2 Error: No image provided"
    }
}

fun showNotification(context: Context, title: String, message: String) {
    val channelId = "model_results"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = android.app.NotificationChannel(
            channelId,
            "Model Results",
            android.app.NotificationManager.IMPORTANCE_DEFAULT
        )
        val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
        NotificationManagerCompat.from(context).notify(System.currentTimeMillis().toInt(), notification)
    }
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

val defaultCategories = listOf(
    Category("Work", "#2196F3"),
    Category("Personal", "#4CAF50"),
    Category("Urgent", "#F44336"),
    Category("Other", "#9C27B0")
)

fun loadCategories(sharedPreferences: SharedPreferences): List<Category> {
    val json = sharedPreferences.getString("categories", null)
    return if (json != null) {
        try {
            Json.decodeFromString<List<Category>>(json)
        } catch (e: Exception) {
            defaultCategories
        }
    } else {
        defaultCategories
    }
}

fun saveCategories(sharedPreferences: SharedPreferences, categories: List<Category>) {
    val json = Json.encodeToString(categories)
    sharedPreferences.edit {
        putString("categories", json)
    }
}

val predefinedColors = listOf(
    Pair("Red", "#F44336"),
    Pair("Green", "#4CAF50"),
    Pair("Blue", "#2196F3"),
    Pair("Purple", "#9C27B0"),
    Pair("Orange", "#FF9800"),
    Pair("Yellow", "#FFEB3B"),
    Pair("Pink", "#E91E63"),
    Pair("Teal", "#009688")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowHomeScreen(
    workflows: List<Workflow>,
    categories: List<Category>,
    workflowCounter: Int,
    onWorkflowsChanged: (List<Workflow>) -> Unit,
    onCategoriesChanged: (List<Category>) -> Unit,
    onWorkflowCounterChanged: (Int) -> Unit,
    onWorkflowSelected: (Int) -> Unit,
    onToggleTheme: () -> Unit,
    onModelScreen: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var newWorkflowName by remember { mutableStateOf("") }
    var initialAction by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var newCategoryName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(predefinedColors.first()) }
    var showDeleteSnackbar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filterCategory by remember { mutableStateOf<String?>(null) }
    var isFilterDropdownExpanded by remember { mutableStateOf(false) }
    var isColorDropdownExpanded by remember { mutableStateOf(false) }

    val filteredWorkflows = workflows.filter {
        it.name.contains(searchQuery, ignoreCase = true) &&
                (filterCategory == null || it.category?.name == filterCategory)
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
                .padding(bottom = Spacing.small),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        ExposedDropdownMenuBox(
            expanded = isFilterDropdownExpanded,
            onExpandedChange = { isFilterDropdownExpanded = !isFilterDropdownExpanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.medium)
        ) {
            OutlinedTextField(
                value = filterCategory ?: "All Categories",
                onValueChange = {},
                readOnly = true,
                label = { Text("Filter by Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFilterDropdownExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            ExposedDropdownMenu(
                expanded = isFilterDropdownExpanded,
                onDismissRequest = { isFilterDropdownExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("All Categories") },
                    onClick = {
                        filterCategory = null
                        isFilterDropdownExpanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
                categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            filterCategory = category.name
                            isFilterDropdownExpanded = false
                        },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                DropdownMenuItem(
                    text = { Text("Manage Categories") },
                    onClick = {
                        showManageCategoriesDialog = true
                        isFilterDropdownExpanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        FilledIconButton(
            onClick = { showCategoryDialog = true },
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = Spacing.small)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Category",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

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
            Row {
                IconButton(
                    onClick = onToggleTheme,
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
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onModelScreen,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = "Model Invocation",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
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
                        selectedCategory = workflow.category
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
                selectedCategory = null
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
            selectedCategory = selectedCategory,
            categories = categories,
            onWorkflowNameChange = { newWorkflowName = it },
            onInitialActionChange = { initialAction = it },
            onCategoryChange = { selectedCategory = it },
            onConfirm = {
                val newWorkflow = Workflow(
                    name = newWorkflowName,
                    actions = if (initialAction.isNotBlank()) listOf(initialAction) else emptyList(),
                    category = selectedCategory
                )
                onWorkflowsChanged(workflows + newWorkflow)
                onWorkflowCounterChanged(workflowCounter + 1)
                showCreateDialog = false
            },
            onDismissRequest = { showCreateDialog = false },
            isConfirmEnabled = newWorkflowName.isNotBlank()
        )
    }

    if (showEditDialog) {
        WorkflowDialog(
            title = "Edit Workflow",
            workflowName = editText,
            selectedCategory = selectedCategory,
            categories = categories,
            onWorkflowNameChange = { editText = it },
            onCategoryChange = { selectedCategory = it },
            onConfirm = {
                onWorkflowsChanged(workflows.toMutableList().also {
                    it[editingIndex] = it[editingIndex].copy(name = editText, category = selectedCategory)
                })
                showEditDialog = false
            },
            onDismissRequest = { showEditDialog = false },
            isConfirmEnabled = editText.isNotBlank(),
            isCancelEnabled = true
        )
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(Spacing.small))
                    ExposedDropdownMenuBox(
                        expanded = isColorDropdownExpanded,
                        onExpandedChange = { isColorDropdownExpanded = !isColorDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedColor.first,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Color") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isColorDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = isColorDropdownExpanded,
                            onDismissRequest = { isColorDropdownExpanded = false }
                        ) {
                            predefinedColors.forEach { color ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(
                                                        color = Color(android.graphics.Color.parseColor(color.second)),
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(color.first)
                                        }
                                    },
                                    onClick = {
                                        selectedColor = color
                                        isColorDropdownExpanded = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            val newCategory = Category(newCategoryName, selectedColor.second)
                            onCategoriesChanged(categories + newCategory)
                            showCategoryDialog = false
                        }
                    },
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showManageCategoriesDialog) {
        AlertDialog(
            onDismissRequest = { showManageCategoriesDialog = false },
            title = { Text("Manage Categories") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    itemsIndexed(categories) { _, category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.small),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            color = Color(android.graphics.Color.parseColor(category.color)),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(category.name)
                            }
                            IconButton(
                                onClick = {
                                    onCategoriesChanged(categories.filter { it != category })
                                    onWorkflowsChanged(
                                        workflows.map {
                                            if (it.category == category) it.copy(category = null) else it
                                        }
                                    )
                                    CoroutineScope(Dispatchers.Main).launch {
                                        snackbarHostState.showSnackbar("Category deleted")
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Category",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showManageCategoriesDialog = false }) {
                    Text("Done")
                }
            }
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
        animationSpec = tween(durationMillis = 150),
        label = "scale"
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
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = workflow.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                workflow.category?.let { category ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(android.graphics.Color.parseColor(category.color)),
                        modifier = Modifier
                            .background(
                                color = Color(android.graphics.Color.parseColor(category.color)).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowDialog(
    title: String,
    workflowName: String,
    initialAction: String = "",
    selectedCategory: Category? = null,
    categories: List<Category>,
    onWorkflowNameChange: (String) -> Unit,
    onInitialActionChange: (String) -> Unit = {},
    onCategoryChange: (Category?) -> Unit,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    isConfirmEnabled: Boolean,
    isCancelEnabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
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
                Spacer(modifier = Modifier.height(Spacing.medium))
                if (initialAction.isNotEmpty() || title == "Create New Workflow") {
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
                    Spacer(modifier = Modifier.height(Spacing.medium))
                }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "No Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No Category") },
                            onClick = {
                                onCategoryChange(null)
                                expanded = false
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(
                                                    color = Color(android.graphics.Color.parseColor(category.color)),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(category.name)
                                    }
                                },
                                onClick = {
                                    onCategoryChange(category)
                                    expanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
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
                Text("OK")
            }
        },
        dismissButton = {
            if (isCancelEnabled) {
                TextButton(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Cancel")
                }
            }
        }
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
    onCategoryChange: (Category?) -> Unit,
    categories: List<Category>,
    onAddCategory: (Category) -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(workflow.name) }
    var newAction by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(workflow.category) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(predefinedColors.first()) }
    var expanded by remember { mutableStateOf(false) }
    var isColorDropdownExpanded by remember { mutableStateOf(false) }
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

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "No Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("No Category") },
                            onClick = {
                                onCategoryChange(null)
                                selectedCategory = null
                                expanded = false
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(
                                                    color = Color(android.graphics.Color.parseColor(category.color)),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(category.name)
                                    }
                                },
                                onClick = {
                                    onCategoryChange(category)
                                    selectedCategory = category
                                    expanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Create New Category") },
                            onClick = {
                                showCategoryDialog = true
                                expanded = false
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

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

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("New Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(Spacing.small))
                    ExposedDropdownMenuBox(
                        expanded = isColorDropdownExpanded,
                        onExpandedChange = { isColorDropdownExpanded = !isColorDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedColor.first,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Color") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isColorDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = isColorDropdownExpanded,
                            onDismissRequest = { isColorDropdownExpanded = false }
                        ) {
                            predefinedColors.forEach { color ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(
                                                        color = Color(android.graphics.Color.parseColor(color.second)),
                                                        shape = RoundedCornerShape(4.dp)
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(color.first)
                                        }
                                    },
                                    onClick = {
                                        selectedColor = color
                                        isColorDropdownExpanded = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            val newCategory = Category(newCategoryName, selectedColor.second)
                            onAddCategory(newCategory)
                            onCategoryChange(newCategory)
                            selectedCategory = newCategory
                            showCategoryDialog = false
                        }
                    },
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
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
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "scale"
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
        enabled = enabled,
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
        animationSpec = tween(durationMillis = 150),
        label = "scale"
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



