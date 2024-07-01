package servletspack;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@SuppressWarnings("serial")
public class DistrictDataDownload extends HttpServlet {
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String table_name = request.getParameter("tablename");
		String dist = request.getParameter("distname");
		String bc_cd = request.getParameter("bccdname");
		String job_type = request.getParameter("jobtypename");
		String transfer_dt = request.getParameter("transferdtname");
		String dialer = request.getParameter("dialername");
		
		response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=\"TeleCalling_dtl_" + dist + "_" + bc_cd + "_" + transfer_dt + ".xlsx\"");
		
		String dbURL = "jdbc:oracle:thin:@//10.40.2.250:1521/PDB1";
		String dbUsername = "ltreg";
		String dbPassword = "reg93new";
		
		try {
            Connection connection = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT CONS_NUM AS \"Consumer No\", CONS_NAME AS \"Name\", NVL(NVL(MOB_NO, TELNO3), TELNO2) AS \"Contact No\", TOT_OS_AMT AS \"O/S Amount(Rs.)\", PMNT_AMT AS \"Payment Amount(Rs.)\", LAST_PMNT_DT AS \"Last Payment Date\" FROM " + table_name + " WHERE DIST='" + dist + "' AND BC_CD='" + bc_cd + "' AND JOB_TYPE='" + job_type + "' AND TRUNC(TRANSFER_DT)=TO_DATE('" + transfer_dt + "', 'DD-MON-YY') AND NVL(DEST_MKR, 'CNX') LIKE '" + dialer + "' ORDER BY CONS_NUM");
            
            XSSFWorkbook workbook = new XSSFWorkbook();
            XSSFSheet sheet = workbook.createSheet(dist + " " + bc_cd + " TeleCalling Details");
            
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
            statement.close();
            connection.close();
            
            workbook.write(response.getOutputStream());
            response.getOutputStream().close();
            workbook.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
