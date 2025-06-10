package com.simki.workflowapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simki.workflowapp.ui.theme.WorkflowAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WorkflowAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WorkflowApp()
                }
            }
        }
    }
}

// Data model for a workflow with a name and list of actions
data class Workflow(
    val name: String,
    val actions: List<String> = emptyList()
)

@Composable
fun WorkflowApp() {
    var selectedWorkflowIndex by remember { mutableIntStateOf(-1) }
    var workflows by remember { mutableStateOf(listOf<Workflow>()) }

    if (selectedWorkflowIndex >= 0) {
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
            }
        )
    } else {
        WorkflowHomeScreen(
            workflows = workflows,
            onWorkflowsChanged = { workflows = it },
            onWorkflowSelected = { selectedWorkflowIndex = it }
        )
    }
}

@Composable
fun WorkflowHomeScreen(
    workflows: List<Workflow>,
    onWorkflowsChanged: (List<Workflow>) -> Unit,
    onWorkflowSelected: (Int) -> Unit
) {
    var workflowCounter by remember { mutableIntStateOf(1) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    var editingIndex by remember { mutableIntStateOf(-1) }
    var newWorkflowName by remember { mutableStateOf("") }
    var initialAction by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFFF9F9F9))
    ) {
        Text(
            text = "My Workflows",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(workflows.indices.toList()) { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).clickable {
                            editingIndex = index
                            editText = workflows[index].name
                            showEditDialog = true
                        }) {
                            Text(
                                text = workflows[index].name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        IconButton(onClick = { onWorkflowSelected(index) }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Edit")
                        }
                        IconButton(onClick = {
                            onWorkflowsChanged(workflows.toMutableList().also { it.removeAt(index) })
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }

        Button(
            onClick = {
                newWorkflowName = "Workflow #$workflowCounter"
                initialAction = ""
                showCreateDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
        ) {
            Text("Create New Workflow", color = Color.White)
        }
    }

    // Dialog for creating a new workflow
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Workflow") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newWorkflowName,
                        onValueChange = { newWorkflowName = it },
                        singleLine = true,
                        label = { Text("Workflow Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = initialAction,
                        onValueChange = { initialAction = it },
                        singleLine = true,
                        label = { Text("Initial Action (e.g., Send Email)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newWorkflow = Workflow(
                        name = newWorkflowName,
                        actions = if (initialAction.isNotBlank()) listOf(initialAction) else emptyList()
                    )
                    onWorkflowsChanged(workflows + newWorkflow)
                    workflowCounter++
                    showCreateDialog = false
                }) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog for editing workflow name
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Workflow") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    singleLine = true,
                    label = { Text("Workflow Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onWorkflowsChanged(workflows.toMutableList().also {
                        it[editingIndex] = it[editingIndex].copy(name = editText)
                    })
                    showEditDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DetailedWorkflowEditor(
    workflow: Workflow,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onAddAction: (String) -> Unit
) {
    var name by remember { mutableStateOf(workflow.name) }
    var newAction by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("Back")
            }
            Text(
                text = "Workflow Editor",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(64.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                onRename(it)
            },
            label = { Text("Workflow Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Actions:", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(workflow.actions) { action ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0))
                ) {
                    Text(
                        text = action,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newAction,
                onValueChange = { newAction = it },
                label = { Text("Add Action") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = {
                if (newAction.isNotBlank()) {
                    onAddAction(newAction)
                    newAction = ""
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Action")
            }
        }
    }
}