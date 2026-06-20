package com.iht.document.parser;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Apache PDFBox를 사용해 업로드된 PDF 파일에서 텍스트를 추출하는 컴포넌트.
 *
 * [확장 포인트]
 * 지금은 텍스트 레이어가 있는 일반 PDF만 처리한다. 스캔본(이미지) PDF는 텍스트가
 * 거의 추출되지 않는데, 그런 경우를 지원하려면 OCR(예: Tesseract) 처리를
 * 이 클래스 안에 추가하거나 별도 구현체로 분리하면 된다. 호출하는 쪽(DocumentServiceImpl)은
 * 이 클래스의 시그니처만 알면 되므로 영향을 받지 않는다.
 */
@Component
public class PdfTextExtractor {

    /**
     * PDF 파일에서 전체 텍스트와 총 페이지 수를 추출한다.
     *
     * IN  : file - 사용자가 업로드한 PDF 원본 파일
     * OUT : ExtractResult - 추출된 전체 텍스트(text)와 총 페이지 수(pageCount)
     *
     * @throws IOException PDF 파싱에 실패한 경우 (파일 손상, 암호화 등)
     */
    public ExtractResult extract(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            // 페이지 내 텍스트 위치(좌표) 기준으로 정렬해서 추출 -> 단/표가 섞인 약관도
            // 사람이 읽는 순서에 가깝게 텍스트가 나오도록 함
            stripper.setSortByPosition(true);

            String text = stripper.getText(document);
            int pageCount = document.getNumberOfPages();

            return new ExtractResult(text, pageCount);
        }
    }

    /**
     * PDF 추출 결과를 담는 record.
     * text      : PDF 전체에서 추출된 순수 텍스트
     * pageCount : PDF 총 페이지 수
     */
    public record ExtractResult(String text, int pageCount) {}
}
