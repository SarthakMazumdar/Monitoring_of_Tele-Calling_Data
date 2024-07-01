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
public class SummaryDownload extends HttpServlet {
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String table_name = request.getParameter("tablenamesum");
        String formatted_from_date = request.getParameter("datefromsum");
        String formatted_to_date = request.getParameter("datetosum");
        String dialer_name = request.getParameter("dialernamesum");
        
        response.setContentType("application/vnd.ms-excel");
        response.setHeader("Content-Disposition", "attachment; filename=\"TeleCalling_sum_" + formatted_from_date + "_to_" + formatted_to_date + ".xlsx\"");

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet(formatted_from_date + " to " + formatted_to_date + " TeleCalling Summary");

        String dbURL = "jdbc:oracle:thin:@//10.40.2.250:1521/PDB1";
		String dbUsername = "ltreg";
		String dbPassword = "reg93new";
        
        try {
            Connection connection = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT DIST AS \"District\", BC_CD AS \"Bill Cycle\", JOB_TYPE AS \"Job Type\", TRUNC(TRANSFER_DT) AS \"Transfer Date\", COUNT(CONS_ID) AS \"Total Cons Count\", SUM(TOT_OS_AMT) AS \"Total O/S Amount\", SUM(NVL(PMNT_AMT, 0)) AS \"Total Payment Amount\", NVL(DEST_MKR, 'CNX') AS \"Dialer\" FROM " + table_name + " WHERE TRUNC(TRANSFER_DT) BETWEEN TO_DATE('" + formatted_from_date + "', 'DD-MON-YY') AND TO_DATE('" + formatted_to_date + "', 'DD-MON-YY') AND NVL(DEST_MKR, 'CNX') LIKE '" + dialer_name + "' GROUP BY DIST, BC_CD, JOB_TYPE, TRUNC(TRANSFER_DT), NVL(DEST_MKR, 'CNX') ORDER BY DIST, BC_CD, TRUNC(TRANSFER_DT), JOB_TYPE");

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
                	if (i == 4) {
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
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}