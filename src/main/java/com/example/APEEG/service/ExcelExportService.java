package com.example.APEEG.service;

import com.example.APEEG.model.Instrument;
import com.example.APEEG.model.IssueRecord;
import com.example.APEEG.repository.InstrumentRepository;
import com.example.APEEG.repository.IssueRecordRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExcelExportService {

    private final InstrumentRepository instrumentRepository;
    private final IssueRecordRepository issueRecordRepository;

    public ExcelExportService(InstrumentRepository instrumentRepository,
                              IssueRecordRepository issueRecordRepository) {
        this.instrumentRepository = instrumentRepository;
        this.issueRecordRepository = issueRecordRepository;
    }

    /**
     * Generates an Excel workbook containing all instruments owned by the given scientist ID.
     */
    public byte[] generateInstrumentsExcelReport(String scientistId) {
        List<Instrument> instruments = instrumentRepository.findByOwnerScientistId(scientistId);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("My Instruments");

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Asset ID", "Name", "Make", "Serial No", "Quantity", "Location", "Status", "Calibration Valid To", "Purchase Date", "Purchase Cost"};

            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Populate Data Rows
            int rowIdx = 1;
            for (Instrument inst : instruments) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(inst.getAssetId() != null ? inst.getAssetId() : "");
                row.createCell(1).setCellValue(inst.getName() != null ? inst.getName() : "");
                row.createCell(2).setCellValue(inst.getMake() != null ? inst.getMake() : "");
                row.createCell(3).setCellValue(inst.getSerialNo() != null ? inst.getSerialNo() : "");
                row.createCell(4).setCellValue(inst.getQuantity() != null ? inst.getQuantity() : 1);
                row.createCell(5).setCellValue(inst.getLocation() != null ? inst.getLocation() : "");
                row.createCell(6).setCellValue(inst.getStatus() != null ? inst.getStatus().name() : "");
                row.createCell(7).setCellValue(inst.getCalibrationValidTo() != null ? inst.getCalibrationValidTo().toString() : "");
                row.createCell(8).setCellValue(inst.getPurchaseDate() != null ? inst.getPurchaseDate().toString() : "");
                row.createCell(9).setCellValue(inst.getPurchaseCost() != null ? inst.getPurchaseCost().doubleValue() : 0.0);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Instruments Excel report: " + e.getMessage(), e);
        }
    }

    /**
     * Generates an Excel workbook containing all issue/loan records belonging to the given scientist ID (as Borrower or Owner).
     */
    public byte[] generateIssueRecordsExcelReport(String scientistId) {
        List<IssueRecord> allRecords = issueRecordRepository.findAll();

        // Filter records belonging strictly to the authenticated scientist
        List<IssueRecord> scientistRecords = allRecords.stream().filter(record -> {
            boolean isBorrower = record.getBorrowerScientist() != null
                    && scientistId.equals(record.getBorrowerScientist().getId());
            boolean isOwner = record.getOwnerScientist() != null
                    && scientistId.equals(record.getOwnerScientist().getId());
            return isBorrower || isOwner;
        }).collect(Collectors.toList());

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("My Issue Records");

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Record ID", "Instrument Name", "Asset ID", "Borrower Name", "Owner Name", "Staff Name", "Issue Date", "Due Date", "Actual Return Date", "State", "Purpose", "Condition Out", "Condition In"};

            CellStyle headerStyle = createHeaderStyle(workbook);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Populate Data Rows
            int rowIdx = 1;
            for (IssueRecord rec : scientistRecords) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(rec.getId() != null ? rec.getId() : "");
                row.createCell(1).setCellValue(rec.getInstrument() != null ? rec.getInstrument().getName() : "");
                row.createCell(2).setCellValue(rec.getInstrument() != null ? rec.getInstrument().getAssetId() : "");
                row.createCell(3).setCellValue(rec.getBorrowerScientist() != null ? rec.getBorrowerScientist().getName() : "");
                row.createCell(4).setCellValue(rec.getOwnerScientist() != null ? rec.getOwnerScientist().getName() : "");
                row.createCell(5).setCellValue(rec.getStaffName() != null ? rec.getStaffName() : "");
                row.createCell(6).setCellValue(rec.getIssueDate() != null ? rec.getIssueDate().toString() : "");
                row.createCell(7).setCellValue(rec.getDueDate() != null ? rec.getDueDate().toString() : "");
                row.createCell(8).setCellValue(rec.getActualReturnDate() != null ? rec.getActualReturnDate().toString() : "");
                row.createCell(9).setCellValue(rec.getState() != null ? rec.getState().name() : "");
                row.createCell(10).setCellValue(rec.getPurpose() != null ? rec.getPurpose() : "");
                row.createCell(11).setCellValue(rec.getConditionOut() != null ? rec.getConditionOut() : "");
                row.createCell(12).setCellValue(rec.getConditionIn() != null ? rec.getConditionIn() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Issue Records Excel report: " + e.getMessage(), e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.BLUE_GREY.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}