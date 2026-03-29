package com.iverson.aiagent.tool;

import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.iverson.aiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class PDFGenerationTool {

    @Tool(description = "Generate a PDF file with given content")
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {

        // 1. 使用 Paths.get 构建平台无关的路径
        String baseDir = FileConstant.FILE_SAVE_DIR;
        Path pdfDir = Paths.get(baseDir, "pdf");
        Path filePath = pdfDir.resolve(fileName);

        try {
            // 2. 确保目录存在
            Files.createDirectories(pdfDir);

            // 3. 生成 PDF
            try (PdfWriter writer = new PdfWriter(filePath.toFile());
                 PdfDocument pdfDoc = new PdfDocument(writer);
                 Document document = new Document(pdfDoc)) {

                // 4. 加载中文字体（容错处理）
                PdfFont font = loadChineseFont();
                document.setFont(font);

                // 5. 添加内容（支持多行）
                Paragraph paragraph = new Paragraph(content);
                document.add(paragraph);
            }

            return "PDF generated successfully to: " + filePath.toString();

        } catch (IOException e) {
            // 记录详细错误信息
            return "Error generating PDF: " + e.getMessage();
        } catch (Exception e) {
            return "Unexpected error: " + e.getMessage();
        }
    }

    /**
     * 加载中文字体，优先使用内置中文字体，失败则尝试系统字体，再失败则使用默认字体（可能会乱码但不会崩溃）
     */
    private PdfFont loadChineseFont() {
        // 方式一：使用 iText 自带的中文字体（需要 itext-asian 依赖）
        try {
            PdfFont chineseFont = PdfFontFactory.createFont("STSongStd-Light", "Identity-H");
        } catch (IOException e) {
            // 降级：尝试系统常用中文字体路径
            try {
                // Windows 常见中文字体
                String[] fontPaths = {
                        "C:/Windows/Fonts/simsun.ttc",     // 宋体
                        "C:/Windows/Fonts/msyh.ttc",       // 微软雅黑
                        "/System/Library/Fonts/PingFang.ttc", // macOS
                        "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf" // Linux
                };
                for (String path : fontPaths) {
                    if (Files.exists(Paths.get(path))) {
                        return PdfFontFactory.createFont(path, "Identity-H");
                    }
                }
            } catch (IOException ignored) {
            }
            // 最后降级：使用默认字体（可能不支持中文，但至少不会抛异常）
            try {
                return PdfFontFactory.createFont();
            } catch (IOException ex) {
                throw new RuntimeException("No font available for PDF generation", ex);
            }
        }
        return null;
    }
}