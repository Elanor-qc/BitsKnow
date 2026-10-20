package com.gao.xizhi.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gao.xizhi.data.AppDatabase
import com.gao.xizhi.data.KnowledgeCard

private val typeLabels = mapOf(
    KnowledgeCard.Type.CONCEPT to "概念",
    KnowledgeCard.Type.TERM to "术语",
    KnowledgeCard.Type.MISCONCEPTION to "误区",
    KnowledgeCard.Type.METHOD to "方法",
)

@Composable
fun ReviewScreen(db: AppDatabase) {
    val vm: ReviewViewModel = viewModel(factory = ReviewViewModel.Factory(db))
    val modules by vm.modules.collectAsState()
    val cards by vm.cards.collectAsState()
    val ui by vm.ui.collectAsState()
    var pendingDelete by remember { mutableStateOf<KnowledgeCard?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("复盘", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = ui.search,
            onValueChange = { vm.setSearch(it) },
            placeholder = { Text("搜索卡片…") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
        ) {
            FilterChip(
                selected = ui.moduleId == null,
                onClick = { vm.selectModule(null) },
                label = { Text("全部") },
            )
            modules.forEach { module ->
                FilterChip(
                    selected = ui.moduleId == module.id,
                    onClick = { vm.selectModule(module.id) },
                    label = { Text(module.title) },
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp),
        ) {
            FilterChip(
                selected = ui.typeFilter == null,
                onClick = { vm.setTypeFilter(null) },
                label = { Text("全部类型") },
            )
            typeLabels.forEach { (type, label) ->
                FilterChip(
                    selected = ui.typeFilter == type,
                    onClick = { vm.setTypeFilter(if (ui.typeFilter == type) null else type) },
                    label = { Text(label) },
                )
            }
        }

        if (cards.isEmpty()) {
            Text(
                "还没有知识卡片。去「对话」里学点什么吧。",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 32.dp),
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cards, key = { it.id }) { card ->
                    CardItem(
                        card = card,
                        moduleName = modules.find { it.id == card.moduleId }?.title ?: "",
                        onDelete = { pendingDelete = card },
                    )
                }
            }
        }
    }

    pendingDelete?.let { card ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除卡片「${card.title}」？") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteCard(card.id)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun CardItem(
    card: KnowledgeCard,
    moduleName: String,
    onDelete: () -> Unit,
) {
    var expanded by remember(card.id) { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text(typeLabels[card.type] ?: card.type.name) },
                )
                Text(
                    card.title,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                }
            }
            Text(
                moduleName,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (expanded) {
                Text(
                    card.body,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            } else {
                Text(
                    card.body,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
