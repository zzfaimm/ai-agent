package com.iverson.aiagent.tool;

import com.iverson.aiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.nio.file.Paths;

public class FileOperationTool {
    // 假设 FILE_SAVE_DIR 已经定义，例如 "D:/AI/idea/code/ai-agent/tmp"
    private final String baseDir = FileConstant.FILE_SAVE_DIR;
    private final String fileDir = Paths.get(baseDir, "file").toString();

    @Tool(description = "Write content to a file")
    public String writeFile(@ToolParam(description = "Name of the file to write") String fileName,
                            @ToolParam(description = "Content to write to the file") String content) {
        try {
            // 使用 Paths.get 构建完整路径
            java.nio.file.Path path = Paths.get(fileDir, fileName);
            // 确保父目录存在
            java.nio.file.Files.createDirectories(path.getParent());
            // 写入文件
            java.nio.file.Files.writeString(path, content);
            return "File written successfully to: " + path.toString();
        } catch (Exception e) {
            return "Error writing to file: " + e.getMessage();
        }
    }

    @Tool(description = "Read content from a file")
    public String readFile(@ToolParam(description = "Name of the file to read") String fileName) {
        try {
            java.nio.file.Path path = Paths.get(fileDir, fileName);
            if (!java.nio.file.Files.exists(path)) {
                return "File not found: " + path.toString();
            }
            return java.nio.file.Files.readString(path);
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }
}