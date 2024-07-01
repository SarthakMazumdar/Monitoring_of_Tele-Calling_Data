package servletspack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class DistrictZipDownload extends HttpServlet {
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String table_name = request.getParameter("tablenamezip");
		String dialer_type = request.getParameter("dialernamezip");
		String rowcount = request.getParameter("rows");
		String fromdate = request.getParameter("datefromzip");
		String todate = request.getParameter("datetozip");
		int rows = Integer.parseInt(rowcount);
		String[] dists = new String[rows];
		String[] bc_cds = new String[rows];
		String[] job_types = new String[rows];
		String[] transfer_dts = new String[rows];
		
		String dialer;
		
		if (dialer_type.equals("All")) {
			dialer = "%";
		}
		else {
			dialer = dialer_type + "%";
		}
		
		for (int i=0; i<rows; i++) {
			dists[i] = request.getParameter("dist" + Integer.toString(i));
			bc_cds[i] = request.getParameter("bccd" + Integer.toString(i));
			job_types[i] = request.getParameter("jobtype" + Integer.toString(i));
			transfer_dts[i] = request.getParameter("transfer" + Integer.toString(i));
		}
		
		response.setContentType("application/zip");
	    response.setHeader("Content-Disposition", "attachment; filename=\"TeleCalling_dtl_" + fromdate + "_" + todate + "_" + dialer_type + ".zip\"");
	    
	    String dbURL = "jdbc:oracle:thin:@//10.40.2.250:1521/PDB1";
		String dbUsername = "ltreg";
		String dbPassword = "reg93new";
		
		try {
			Connection connection = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
			Statement[] stArr = new Statement[rows];
			ResultSet[] rsArr = new ResultSet[rows];
			ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream());
			String filename;
			
			for (int i=0; i<rows; i++) {
				stArr[i] = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				rsArr[i] = stArr[i].executeQuery("SELECT CONS_NUM AS \"Consumer No\", CONS_NAME AS \"Name\", NVL(NVL(MOB_NO, TELNO3), TELNO2) AS \"Contact No\", TOT_OS_AMT AS \"O/S Amount(Rs.)\", PMNT_AMT AS \"Payment Amount(Rs.)\", LAST_PMNT_DT AS \"Last Payment Date\" FROM " + table_name + " WHERE DIST='" + dists[i] + "' AND BC_CD='" + bc_cds[i] + "' AND JOB_TYPE='" + job_types[i] + "' AND TRUNC(TRANSFER_DT)=TO_DATE('" + transfer_dts[i] + "', 'DD-MON-YY') AND NVL(DEST_MKR, 'CNX') LIKE '" + dialer + "' ORDER BY CONS_NUM");
				filename = "TeleCalling_dtl_" + dists[i] + "_" + bc_cds[i] + "_" + transfer_dts[i] + ".xlsx";
				generateAndAddToZip(rsArr[i], filename, zipOut);
			}
			
			zipOut.finish();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	private void generateAndAddToZip(ResultSet resultSet, String fileName, ZipOutputStream zipOut) throws IOException, SQLException, ParseException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                XSSFSheet sheet = workbook.createSheet("Sheet1");
                
                DecimalFormat decimalFormat = new DecimalFormat("#0.00");
                DataFormat dataFormat = workbook.createDataFormat();
                int rowNum = 0;
                
                XSSFRow headerRow = sheet.createRow(rowNum++);
                for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    XSSFCell cell = headerRow.createCell(i - 1);
                    cell.setCellValue(resultSet.getMetaData().getColumnLabel(i));
                }
                
                XSSFCellStyle decimalCellStyle = workbook.createCellStyle();
                decimalCellStyle.setDataFormat(dataFormat.getFormat("0.00"));
                
                DateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
    			DateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy");
                
                while (resultSet.next()) {
                    XSSFRow row = sheet.createRow(rowNum++);
                    for (int i = 1; i <= resultSet.getMetaData().getColumnCount(); i++) {
                    	XSSFCell cell = row.createCell(i - 1);
                    	if (i == 6) {
                    		if (resultSet.getDate(i) != null) {
    	                		cell.setCellValue(outputFormat.format(inputFormat.parse(inputFormat.format(resultSet.getDate(i)))));
                    		}
                    		else {
                    			cell.setCellValue(resultSet.getString(i));
                    		}
                    		continue;
                    	}
                    	if (resultSet.getMetaData().getColumnType(i) == Types.NUMERIC || resultSet.getMetaData().getColumnType(i) == Types.DECIMAL) {
                            cell.setCellValue(Double.parseDouble(decimalFormat.format(resultSet.getDouble(i))));
                            cell.setCellStyle(decimalCellStyle);
                        } else {
                            cell.setCellValue(resultSet.getString(i));
                        }
                    }
                    for (int i = 0; i < row.getLastCellNum(); i++) {
                        sheet.autoSizeColumn(i);
                    }
                }
                resultSet.close();
                workbook.write(baos);
                workbook.close();
            }
            zipOut.putNextEntry(new ZipEntry(fileName));
            zipOut.write(baos.toByteArray());
            zipOut.closeEntry();
        }
    }
}
