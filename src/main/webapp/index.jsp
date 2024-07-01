<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1" %>

<%@ page import="dbconnection.LoadDriver" %>
<%@ page import="java.sql.CallableStatement" %>
<%@ page import="java.sql.Connection" %>
<%@ page import="java.sql.DriverManager" %>
<%@ page import="java.sql.ResultSet" %>
<%@ page import="java.sql.Statement" %>
<%@ page import="java.sql.SQLRecoverableException" %>
<%@ page import="java.sql.Types" %>
<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>

<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Consumer Data</title>
<link href="css/temp14.css" rel="stylesheet">
<script type="text/javascript">
	function dateSet() {
		document.getElementById("to-date").value = new Date().toISOString().substring(0, 10);
		let today_date = new Date();
	    let twenty_days_ago_date = new Date(today_date);
	    twenty_days_ago_date.setDate(today_date.getDate() - 19);
	    document.getElementById("from-date").value = twenty_days_ago_date.toISOString().substring(0, 10);
	}
	
	document.addEventListener("DOMContentLoaded", function() {
		dateSet()
	    
	    document.getElementById('filterForm').addEventListener('submit', function(e) {
	    	e.preventDefault();
	    	let table_filter = document.getElementById('data-table-filter').value;
	        let from_date = new Date(document.getElementById('from-date').value);
	        let to_date = new Date(document.getElementById('to-date').value);
		    let today = new Date();
		    let twenty_days_ago = new Date(today);
		    twenty_days_ago.setDate(today.getDate() - 20);
		    
		    if (from_date >= today || to_date >= today) {
		    	alert("Future dates cannot be selected.");
		    	return;
		    }
		    
		    if (to_date <= from_date) {
		    	alert("Invalid date range.");
		    	return;
		    }
		    
		    if (table_filter == "Current" && from_date < twenty_days_ago) {
		    	alert("Please select date range within 20 days from today.");
		    	return;
		    }
		    
		    if (to_date > new Date(from_date.getTime() + (20 * 24 * 60 * 60 * 1000))) {
		    	alert("Date range must not exceed 20 days.");
		        return;
		    }
		    
		    this.submit();
	    });
	});

	function expandDist(btnId) {
		let tabId = btnId + "1";
		let dwnldId = btnId + "2";
		let btn = document.getElementById(btnId);
		let tab = document.getElementById(tabId);
		let dwnldBtn = document.getElementById(dwnldId);
		dwnldBtn.disabled = false;
		dwnldBtn.style.opacity = '1';
		dwnldBtn.style.cursor = 'pointer';
		dwnldBtn.style.color = "#fff";
		dwnldBtn.style.backgroundColor = "#4caf50";
		dwnldBtn.value = "Download Details";
		if (tab.style.display == "none") {
			tab.style.display = "table-cell";
			btn.innerHTML = "Collapse";
		}
		else {
			tab.style.display = "none";
			btn.innerHTML = "Expand";
		}
		dwnldBtn.addEventListener('mouseover', function() {
			dwnldBtn.style.opacity = '0.8';
		});
		dwnldBtn.addEventListener('mouseout', function() {
			dwnldBtn.style.opacity = '1';
		});
	}
	
	function disableDwnld(dwnldBtnId) {
		let dwnldBtn = document.getElementById(dwnldBtnId);
		setTimeout(function() {
			dwnldBtn.disabled = true;
			dwnldBtn.style.opacity = '0.8'
			dwnldBtn.style.cursor = 'default';
			dwnldBtn.style.color = "black";
			if (dwnldBtnId == "dist-dtl-dwnld") {
				dwnldBtn.style.backgroundColor = "#ffeb3b";
				dwnldBtn.value = "Downloaded ZIP";
			}
			else {
				dwnldBtn.style.backgroundColor = "#ffc107";
				dwnldBtn.value = "Downloaded Details";
			}
		}, 100);
	}
</script>
</head>
<body>
	<header>
		<img src='images/rpsg-logo.png' alt='RPSG Group Logo'>
		<h1>Telecalling Information</h1>
		<img src='images/cesc-logo.png' alt='CESC Logo'>
	</header>
	<section class='container-slim'>
		<form id='filterForm' action='index.jsp' method='post'>
			<div class='form-content'>
				<div class='form-grp'>
					<select name='data-table' class='custom-input' id='data-table-filter'>
						<option value="Current">Current (20 Days)</option>
						<option value="Archive">Archive (Earlier)</option>
					</select>
				</div>
				<div class='form-grp'>
					<label for='from'>Transfer Date From:</label>
					<input type='date' name='from' class='custom-input' id='from-date'>
					<label for='to' class='custom-input'>To:</label>
					<input type='date' name='to' class='custom-input' id='to-date'>
				</div>
				<div class='form-grp'>
					<label for='dialer-select'>Dialer:</label>
					<select name='dialer-select' class='custom-input' id='dialer-filter'>
						<option>CNX</option>
						<option>FUSION</option>
						<option selected>All</option>
					</select>
				</div>
				<div class='form-grp'>
					<button type='submit' class='form-grp btn' id='filter-submit'>Submit</button>
					<button type='button' class='form-grp btn' id='page-close' onclick='window.close();'>Close</button>
				</div>
			</div>
		</form>
	</section>
	<main class="container">
		<% 
		if (request.getParameter("from") != null && request.getParameter("to") != null) {
       		LoadDriver.loadDbDriver();
       	
       		String dbURL = "jdbc:oracle:thin:@//10.40.2.250:1521/PDB1";
			String dbUsername = "ltreg";
			String dbPassword = "reg93new";
		
			String data_table = request.getParameter("data-table");
			String dialer = request.getParameter("dialer-select");
        	String from_date = request.getParameter("from");
        	String to_date = request.getParameter("to");
        	
        	SimpleDateFormat inputDates = new SimpleDateFormat("yyyy-MM-dd");
        	SimpleDateFormat outputDates = new SimpleDateFormat("dd-MMM-yy");
        	
        	Date fromDateType = inputDates.parse(from_date);
        	Date toDateType = inputDates.parse(to_date);
        	String formatted_from_date = outputDates.format(fromDateType).toUpperCase();
        	String formatted_to_date = outputDates.format(toDateType).toUpperCase();
        	
        	String table_name, dialer_name;
            
	        if (data_table.equals("Current")) {
	        	table_name = "QL_CUTOFF_JOB_TEL_FOLLOWUP";
	        }
	        else {
	        	table_name = "OLD_CUTOFF_JOB_TEL_FOLLOWUP";
	        }
	        
	        if (dialer.equals("All")) {
	        	dialer_name = "%";
	        }
	        else {
	        	dialer_name = dialer + "%";
	        }
            
			try (Connection con = DriverManager.getConnection(dbURL, dbUsername, dbPassword)) {
			
				System.out.println("Database Connected Succesfully...");
			
				Statement st = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				ResultSet rs = st.executeQuery("SELECT DIST, BC_CD, JOB_TYPE, TRUNC(TRANSFER_DT), COUNT(CONS_ID), SUM(TOT_OS_AMT), SUM(NVL(PMNT_AMT, 0)), NVL(DEST_MKR, 'CNX') FROM " + table_name + " WHERE TRUNC(TRANSFER_DT) BETWEEN TO_DATE('" + formatted_from_date + "', 'DD-MON-YY') AND TO_DATE('" + formatted_to_date + "', 'DD-MON-YY') AND NVL(DEST_MKR, 'CNX') LIKE '" + dialer_name + "' GROUP BY DIST, BC_CD, JOB_TYPE, TRUNC(TRANSFER_DT), NVL(DEST_MKR, 'CNX') ORDER BY DIST, BC_CD, TRUNC(TRANSFER_DT), JOB_TYPE");
		%>
		<div id='all-content'>
			<p class='table-title'>Showing <strong><%= data_table %></strong> data for the Period <strong><%= formatted_from_date %></strong> to <strong><%= formatted_to_date %></strong> of the Dialer: <strong><%= dialer %></strong></p>
		<%
				if (!rs.next()) {
		%>
			<p class='empty-res'>No Data for the Given Filters</p>
		<%
				}
				else {
					rs.beforeFirst();
					
					int cols = rs.getMetaData().getColumnCount();
					int rows = 0, i = 0;
				
					while (rs.next()) {
						rows++;
					}
					
					rs.beforeFirst();
					
					String[] dists = new String[rows];
					String[] bc_cds = new String[rows];
					String[] job_types = new String[rows];
					String[] transfer_dts = new String[rows];
					String[] dest_mkrs = new String[rows];
							
					while (rs.next()) {
						dists[i] = rs.getString(1);
						bc_cds[i] = rs.getString(2);
						job_types[i] = rs.getString(3);
						transfer_dts[i] = outputDates.format(rs.getDate(4)).toUpperCase();
						dest_mkrs[i] = rs.getString(8);
						i++;
					}
				
					rs.beforeFirst();
					
					Statement[] stArr = new Statement[rows];
					ResultSet[] rsArr = new ResultSet[rows];
					
					for (int j=0; j<rows; j++) {
						stArr[j] = con.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
						rsArr[j] = stArr[j].executeQuery("SELECT CONS_NUM, CONS_NAME, NVL(NVL(MOB_NO, TELNO3), TELNO2), TOT_OS_AMT, PMNT_AMT, LAST_PMNT_DT FROM " + table_name + " WHERE DIST='" + dists[j] + "' AND BC_CD='" + bc_cds[j] + "' AND JOB_TYPE='" + job_types[j] + "' AND TRUNC(TRANSFER_DT)=TO_DATE('" + transfer_dts[j] + "', 'DD-MON-YY') AND NVL(DEST_MKR, 'CNX') LIKE '" + dest_mkrs[j] + "' ORDER BY CONS_NUM");
					}
		%>
			<div class='table-wrapper'>
				<table id='main-table'>
					<thead class='pos-sticky z-zero main-table-head'>
						<tr>
							<th>District</th>
							<th>Bill Cycle</th>
							<th>Job Type</th>
							<th>Transfer Date</th>
							<th>Total Cons Count</th>
							<th>Total O/S Amount</th>
							<th>Total Payment Amount</th>
							<th>Dialer</th>
							<th></th>
							<th></th>
						</tr>
					</thead>
					<tbody>
					<%
					int k=0;
					String btnId = "";
					String tabId = "";
					String dwnldId = "";
					DateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
					DateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy");
					
					while (rs.next()) {
					%>
						<tr>
							<td><%= rs.getString(1) != null ? rs.getString(1) : "" %></td>
							<td><%= rs.getString(2) != null ? rs.getString(2) : "" %></td>
							<td><%= rs.getString(3) != null ? rs.getString(3) : "" %></td>
							<td><%= rs.getDate(4) != null ? outputFormat.format(inputFormat.parse(inputFormat.format(rs.getDate(4)))) : "" %></td>
							<td><%= rs.getInt(5) %></td>
							<td><%= String.format("%.2f", rs.getDouble(6)) != null ? String.format("%.2f", rs.getDouble(6)) : "" %></td>
							<td><%= String.format("%.2f", rs.getDouble(7)) != null ? String.format("%.2f", rs.getDouble(7)) : "" %></td>
							<td><%= rs.getString(8) != null ? rs.getString(8) : "" %></td>
					
						<% 
						   btnId = rs.getString(1) + rs.getString(2) + rs.getString(3) + rs.getString(4) + rs.getString(8);
						   tabId = btnId + "1";
						   dwnldId = btnId + "2";
						%>
					
							<td>
								<button class='btn exp-btn' id='<%= btnId %>' onclick='expandDist(this.id);'>Expand</button>
							</td>
							<td>
								<form action='DistrictDataDownload' method='post'>
									<input type='hidden' name='tablename' value='<%= table_name %>'>
									<input type='hidden' name='distname' value='<%= rs.getString(1) %>'>
									<input type='hidden' name='bccdname' value='<%= rs.getString(2) %>'>
									<input type='hidden' name='jobtypename' value='<%= rs.getString(3) %>'>
									<input type='hidden' name='transferdtname' value='<%= outputDates.format(inputDates.parse(inputDates.format(rs.getDate(4)))).toUpperCase() %>'>
									<input type='hidden' name='dialername' value='<%= dialer_name %>'>
									<input type='submit' class='btn down-btn' id='<%= dwnldId %>' value='Download Details' onclick='disableDwnld(this.id);'>
								</form>
							</td>
						</tr>
					</tbody>
						<tr>
							<td colspan='10' style='display:none;' class='no-pad' id='<%= tabId %>'>
								<table class='inner-table'>
									<thead class='pos-sticky z-one inner-table-head'>
										<tr>
											<th class='var-pad'>Consumer No</th>
											<th class='var-pad'>Name</th>
											<th class='var-pad'>Contact No</th>
											<th class='var-pad'>O/S Amount(Rs.)</th>
											<th class='var-pad'>Payment Amount(Rs.)</th>
											<th class='var-pad'>Last Payment Date</th>
										</tr>
									</thead>
									<tbody class='inner-table-body'>
						<% while (rsArr[k].next()) { %>
										<tr>
											<td class='var-pad'><%= rsArr[k].getString(1) != null ? rsArr[k].getString(1) : "" %></td>
											<td class='var-pad'><%= rsArr[k].getString(2) != null ? rsArr[k].getString(2) : "" %></td>
											<td class='var-pad'><%= rsArr[k].getString(3) != null ? rsArr[k].getString(3) : "" %></td>
											<td class='var-pad'><%= String.format("%.2f", rsArr[k].getDouble(4)) != null ? String.format("%.2f", rsArr[k].getDouble(4)) : "" %></td>
											<td class='var-pad'><%= String.format("%.2f", rsArr[k].getDouble(5)) != null ? String.format("%.2f", rsArr[k].getDouble(5)) : "" %></td>
											<td class='var-pad'><%= rsArr[k].getDate(6) != null ? outputFormat.format(inputFormat.parse(inputFormat.format(rsArr[k].getDate(6)))) : "" %></td>
										</tr>
						<% } %>
									</tbody> 
								</table>
							</td>
						</tr>
						<%
						k++;
					}
					%>
				</table>
			</div>
			<div id='sum-form'>
				<form action='SummaryDownload' method='post'>
					<input type='hidden' name='tablenamesum' value='<%= table_name %>'>
					<input type='hidden' name='datefromsum' value='<%= formatted_from_date %>'>
					<input type='hidden' name='datetosum' value='<%= formatted_to_date %>'>
					<input type='hidden' name='dialernamesum' value='<%= dialer_name %>'>
					<input type='submit' name='downsum' class='btn sum-btn' value='Download Summary'>
				</form>
				<form action='DistrictZipDownload' method='post'>
					<input type='hidden' name='tablenamezip' value='<%= table_name %>'>
					<input type='hidden' name='rows' value='<%= rows %>'>
					<%
					for (int j=0; j<rows; j++) {
						out.print("<input type='hidden' name='dist" + Integer.toString(j) + "' value='" + dists[j] + "'>");
						out.print("<input type='hidden' name='bccd" + Integer.toString(j) + "' value='" + bc_cds[j] + "'>");
						out.print("<input type='hidden' name='jobtype" + Integer.toString(j) + "' value='" + job_types[j] + "'>");
						out.print("<input type='hidden' name='transfer" + Integer.toString(j) + "' value='" + transfer_dts[j] + "'>");
					}
					%>
					<input type='hidden' name='dialernamezip' value='<%= dialer %>'>
					<input type='hidden' name='datefromzip' value='<%= formatted_from_date %>'>
					<input type='hidden' name='datetozip' value='<%= formatted_to_date %>'>
					<input type='submit' name='downdtlall' class='btn sum-btn' id='dist-dtl-dwnld' value='Download All Details (in ZIP)' onclick='disableDwnld(this.id);'>
				</form>
			</div>
		</div>
	</main>
			<% }
			} catch(SQLRecoverableException e) {
					e.printStackTrace();
			%>
	<p class='empty-res'>Database is Currently Not Responding</p>
			<%
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
			%>
</body>
</html>