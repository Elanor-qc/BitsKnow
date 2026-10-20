package com.gao.xizhi.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gao.xizhi.data.ApiConfig
import com.gao.xizhi.data.AppDatabase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(db: AppDatabase) {
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(db))
    val configs by vm.configs.collectAsState()
    val testing by vm.testing.collectAsState()
    val testResult by vm.testResult.collectAsState()
    val exporting by vm.exporting.collectAsState()
    val exportResult by vm.exportResult.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var editing by remember { mutableStateOf<ApiConfig?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var showRenderTest by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = "添加配置")
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("API 配置", style = MaterialTheme.typography.titleLarge)
            Text(
                "OpenAI 兼容接口。支持 DeepSeek、Kimi、GLM、Qwen、OpenAI 等。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            TextButton(onClick = { showRenderTest = true }) { Text("渲染测试") }
            TextButton(
                onClick = { vm.export(context.applicationContext) },
                enabled = !exporting,
            ) { Text(if (exporting) "导出中…" else "导出学习数据") }
            exportResult?.let { result ->
                Text(
                    result,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (configs.isEmpty()) {
                Text("还没有配置，点右下角 + 添加。", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(configs, key = { it.id }) { config ->
                        Card(
                            onClick = { vm.activate(config.id) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (config.isActive) CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ) else CardDefaults.cardColors(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(config.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "${config.model} · ${config.baseUrl}",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                    )
                                }
                                if (config.isActive) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "使用中",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                IconButton(onClick = { vm.test(config) }, enabled = !testing) {
                                    Text("测试", style = MaterialTheme.typography.labelMedium)
                                }
                                IconButton(onClick = { editing = config }) {
                                    Icon(Icons.Filled.Edit, contentDescription = "编辑")
                                }
                                IconButton(onClick = { vm.delete(config.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            testResult?.let { result ->
                Text(
                    result,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }

    if (showRenderTest) {
        com.gao.xizhi.markdown.RenderTestDialog(onDismiss = { showRenderTest = false })
    }

    if (showCreate) {
        ConfigEditorDialog(
            initial = null,
            onDismiss = { showCreate = false },
        ) { config ->
            vm.save(config, isNew = true)
            showCreate = false
        }
    }

    editing?.let { config ->
        ConfigEditorDialog(
            initial = config,
            onDismiss = { editing = null },
        ) { updated ->
            vm.save(updated, isNew = false)
            editing = null
        }
    }
}

@Composable
private fun ConfigEditorDialog(
    initial: ApiConfig?,
    onDismiss: () -> Unit,
    onSave: (ApiConfig) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var baseUrl by remember { mutableStateOf(initial?.baseUrl ?: "") }
    var apiKey by remember { mutableStateOf(initial?.apiKey ?: "") }
    var model by remember { mutableStateOf(initial?.model ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "添加配置" else "编辑配置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("名称") }, singleLine = true)
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    placeholder = { Text("https://api.deepseek.com") },
                    singleLine = true,
                )
                OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, label = { Text("API Key") }, singleLine = true)
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("模型名") },
                    placeholder = { Text("deepseek-chat") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        (initial ?: ApiConfig(name = "", baseUrl = "", apiKey = "", model = "")).copy(
                            name = name.trim(),
                            baseUrl = baseUrl.trim(),
                            apiKey = apiKey.trim(),
                            model = model.trim(),
                        )
                    )
                },
                enabled = name.isNotBlank() && baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank(),
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
