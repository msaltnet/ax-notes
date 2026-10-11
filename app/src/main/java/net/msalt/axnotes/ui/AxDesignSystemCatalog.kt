package net.msalt.axnotes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/** Executable catalogue, shared with tests. Not an additional destination in the reader app. */
@Composable
internal fun AxDesignSystemCatalog(darkTheme: Boolean = false) {
    AxTheme(darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    Modifier.widthIn(max = AxSize.listMaxWidth).fillMaxSize().testTag("design_catalog"),
                    contentPadding = PaddingValues(AxComponentTokens.pageMargin),
                    verticalArrangement = Arrangement.spacedBy(AxComponentTokens.sectionGap)
                ) {
                    item {
                        Text("AX 디자인 시스템", style = MaterialTheme.typography.headlineMedium)
                        Text("명조 제목, 종이와 잉크, 생각을 남기는 여백", style = MaterialTheme.typography.bodyLarge)
                    }
                    item {
                        CatalogSection("행동 · 기본 / 비활성 / 처리 중") {
                            AxButton({}, Modifier.fillMaxWidth().testTag("catalog_primary")) { Text("메모 저장") }
                            AxButton({}, Modifier.fillMaxWidth(), enabled = false) { Text("내용을 입력하세요") }
                            AxButton({}, Modifier.fillMaxWidth(), loading = true) { Text("저장 중…") }
                            AxOutlinedButton({}) { Text("날짜 선택") }
                            AxTextButton({}) { Text("계속 작성") }
                            AxTextButton({}, destructive = true) { Text("메모 삭제") }
                        }
                    }
                    item {
                        CatalogSection("글 목록 · 기본 / 선택") {
                            AxCard({}, Modifier.testTag("catalog_card_default")) {
                                CatalogCardContent("AI와 함께 일하며 배운 것들", "2026.10.10 · 프로젝트")
                            }
                            AxCard({}, Modifier.testTag("catalog_card_selected"), selected = true) {
                                CatalogCardContent("선택한 글은 잉크색 여백선으로 구분합니다", "글을 선택해도 목록과 읽던 위치를 유지합니다")
                            }
                        }
                    }
                    item {
                        CatalogSection("분류 · 선택 / 선택 안 됨 / 비활성") {
                            var selected by remember { mutableStateOf(true) }
                            AxSectionTab(selected, { selected = !selected }, "시리즈", Modifier.testTag("catalog_section_tab"))
                            AxFilterChip(selected, { selected = !selected }, { Text("시리즈") })
                            AxFilterChip(false, {}, { Text("프로젝트의 긴 이름도 알아볼 수 있게") })
                            AxFilterChip(false, {}, { Text("사용할 수 없음") }, enabled = false)
                        }
                    }
                    item {
                        CatalogSection("입력 · 기본 / 오류 / 읽기 전용 / 비활성") {
                            var text by remember { mutableStateOf("") }
                            AxTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text("메모 제목") })
                            AxTextField("", {}, Modifier.fillMaxWidth(), label = { Text("메모 내용") }, isError = true, supportingText = { Text("기억할 내용을 입력해주세요") })
                            AxTextField("저장된 글의 제목", {}, Modifier.fillMaxWidth(), readOnly = true, label = { Text("연결된 글") })
                            AxTextField("잠시 기다려주세요", {}, Modifier.fillMaxWidth(), enabled = false, label = { Text("저장 중인 메모") })
                        }
                    }
                    item {
                        CatalogSection("안내 · 정보 / 주의 / 오류 / 빈 상태") {
                            AxNotice("저장한 본문은 인터넷 없이 다시 읽을 수 있어요")
                            AxNotice("이미지와 외부 링크는 인터넷이 필요할 수 있어요", tone = AxNoticeTone.Warning)
                            AxNotice("다시 연결되면 새 글을 받아보세요. 저장된 글은 그대로예요", tone = AxNoticeTone.Error)
                            AxEmptyState("남긴 메모가 없어요", "글을 열고 기억할 생각을 남겨보세요")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AxComponentTokens.cardGap)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun CatalogCardContent(title: String, supporting: String) {
    Column(Modifier.padding(AxComponentTokens.cardPadding), verticalArrangement = Arrangement.spacedBy(AxSpacing.sm)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
