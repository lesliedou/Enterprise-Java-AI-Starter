package com.enterprise.ai.starter.web.controller;

import com.enterprise.ai.starter.web.dto.ChatApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * 知识库管理控制器
 *
 * @author enterprise-ai-expert
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/rag")
public class RagAdminController {

    /**
     * 上传文档并触发向量化
     */
    @PostMapping("/upload")
    public Mono<ChatApiResponse<String>> uploadDocument(@RequestPart("file") Mono<FilePart> filePart) {
        return filePart.flatMap(file -> {
            log.info("接收到 RAG 文档上传: {}", file.filename());
            // 实际 RAG 流程：
            // 1. 读取文件内容 (Apache Tika)
            // 2. 文本清洗与分段 (RecursiveCharacterTextSplitter)
            // 3. 向量化 (EmbeddingModel)
            // 4. 存入向量库 (VectorStore)
            
            return Mono.just(ChatApiResponse.success("文件 " + file.filename() + " 已进入向量化队列"));
        });
    }
}
