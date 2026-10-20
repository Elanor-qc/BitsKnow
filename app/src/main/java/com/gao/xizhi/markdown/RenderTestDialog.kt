package com.gao.xizhi.markdown

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val SAMPLE = """
# 一级标题：高斯泼溅

普通段落，中英文混排 3D Gaussian Splatting 是 2023 年出现的**新视角合成**方法，用一组*各向异性高斯*表示场景。

## 要点列表

- 显式表示，可实时渲染（`>100 FPS`）
- 优化方式是梯度下降
  - 交替执行 densify 与 prune

## 表格

| 组件 | 作用 |
| --- | --- |
| 3D Gaussians | 场景表示 |
| Splatting | 光栅化 |

## 数学

行内公式高斯函数 ${'$'}f(x) = a e^{-\frac{(x-\mu)^2}{2\sigma^2}}${'$'} 是基础。

损失函数：

${'$'}${'$'}\mathcal{L} = (1-\lambda)\mathcal{L}_{1} + \lambda \mathcal{L}_{\text{D-SSIM}}${'$'}${'$'}

反斜杠定界符：旋转矩阵 \(R^\top R = I\)。

math 代码块：

```math
R = \begin{bmatrix} \mathbf{r}_1 & \mathbf{r}_2 & \mathbf{r}_3 \end{bmatrix}
```

## 代码

```python
for cam in cameras:
    loss = l1(pred(cam), gt(cam))
    loss.backward()
```
"""

@Composable
fun RenderTestDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("渲染测试") },
        text = {
            androidx.compose.foundation.layout.Column(
                Modifier.verticalScroll(rememberScrollState()).fillMaxWidth()
            ) {
                MarkdownText(text = SAMPLE, modifier = Modifier.padding(vertical = 4.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}
