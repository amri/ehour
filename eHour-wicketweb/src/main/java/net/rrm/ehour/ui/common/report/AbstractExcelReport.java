/**
 * Created on Sep 15, 2007
 * Created by Thies Edeling
 * Created by Thies Edeling
 * Copyright (C) 2007 TE-CON, All Rights Reserved.
 *
 * This Software is copyright TE-CON 2007. This Software is not open source by definition. The source of the Software is available for educational purposes.
 * TE-CON holds all the ownership rights on the Software.
 * TE-CON freely grants the right to use the Software. Any reproduction or modification of this Software, whether for commercial use or open source,
 * is subject to obtaining the prior express authorization of TE-CON.
 * 
 * thies@te-con.nl
 * TE-CON
 * Legmeerstraat 4-2h, 1058ND, AMSTERDAM, The Netherlands
 *
 */

package net.rrm.ehour.ui.common.report;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import net.rrm.ehour.ui.common.component.AbstractExcelResource;
import net.rrm.ehour.ui.common.report.excel.CellFactory;
import net.rrm.ehour.ui.common.report.excel.CellStyle;
import net.rrm.ehour.ui.common.session.EhourWebSession;
import net.rrm.ehour.ui.report.TreeReportElement;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellRangeAddress;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

/**
 * Abstract aggregate excel report
 **/
public abstract class AbstractExcelReport extends AbstractExcelResource
{
	private static final long serialVersionUID = 1L;

	private final static Logger logger = Logger.getLogger(AbstractExcelReport.class);
	
	private ReportConfig	reportConfig;
	
	/**
	 * 
	 * @param reportConfig
	 */
	public AbstractExcelReport(ReportConfig reportConfig)
	{
		this.reportConfig = reportConfig;
	}	
	
	/**
	 * Get the excel data, cache once created
	 * @throws IOException 
	 * @throws Exception 
	 */
	@Override
	public byte[] getExcelData(String reportId) throws IOException
	{
		Report report = (Report)EhourWebSession.getSession().getObjectCache().getObjectFromCache(reportId);
		
		logger.trace("Creating excel report");
		HSSFWorkbook workbook = createWorkbook(report);
		byte[] excelData = workbook.getBytes();
		
		return excelData;
	}
	
	/**
	 * Create the workbook
	 * @param treeReport
	 * @return
	 */
	protected HSSFWorkbook createWorkbook(Report treeReport)
	{
		HSSFWorkbook wb = new HSSFWorkbook();
		HSSFSheet 	sheet = wb.createSheet((String)getExcelReportName().getObject());
		int			rowNumber = 0;
		short		column;
		
		for (column = 0; column < 4; column++)
		{
			sheet.setColumnWidth(column, 5000);
		}

		for (; column < 7; column++)
		{
			sheet.setColumnWidth(column, 3000);
		}

		rowNumber = createHeaders(rowNumber, sheet, treeReport, wb);
		
		rowNumber = addColumnHeaders(rowNumber, sheet, wb);
		
		fillReportSheet(treeReport, sheet, rowNumber, wb);
		
		return wb;		
	}
	
	/**
	 * Get report name for the filename
	 * @return
	 */
	protected abstract IModel getExcelReportName();
	
	/**
	 * Get report header
	 * @return
	 */
	protected abstract IModel getHeaderReportName();

	
	/**
	 * Add column headers
	 * @param rowNumber
	 * @param sheet
	 * @return
	 */
	private int addColumnHeaders(int rowNumber, HSSFSheet sheet, HSSFWorkbook workbook)
	{
		HSSFRow		row;
		int			cellNumber = 0;
		IModel		headerModel;
		
		row = sheet.createRow(rowNumber++);
		
		for (ReportColumn reportColumn : reportConfig.getReportColumns())
		{
			if (reportColumn.isVisible())
			{
				headerModel = new ResourceModel(reportColumn.getColumnHeaderResourceKey());
				
				CellFactory.createCell(row, cellNumber++, headerModel, workbook, CellStyle.HEADER);
			}
		} 

		return rowNumber;
	}
	
	
	/**
	 * Fill report sheet
	 * @param reportData
	 * @param sheet
	 * @param rowNumber
	 */
	@SuppressWarnings("unchecked")
	protected void fillReportSheet(Report reportData, HSSFSheet sheet, int rowNumber, HSSFWorkbook workbook)
	{
		List<TreeReportElement> matrix = (List<TreeReportElement>)reportData.getReportData().getReportElements();
		ReportColumn[]	columnHeaders = reportConfig.getReportColumns();
		HSSFRow				row;
		
		for (TreeReportElement element : matrix)
		{
			row = sheet.createRow(rowNumber++);

			addColumns(workbook, columnHeaders, row, element);
		}
	}

	private void addColumns(HSSFWorkbook workbook, ReportColumn[] columnHeaders, HSSFRow row, TreeReportElement element)
	{
		int	i = 0;
		int cellNumber = 0;
		
		
		// add cells for a row
		for (Serializable cellValue : element.getRow())
		{
			if (columnHeaders[i].isVisible() && cellValue != null)
			{
				if (columnHeaders[i].getColumnType() == ReportColumn.ColumnType.HOUR)
				{
					CellFactory.createCell(row, cellNumber++, cellValue, workbook, CellStyle.DIGIT);
				}
				else if (columnHeaders[i].getColumnType() == ReportColumn.ColumnType.TURNOVER
						 || columnHeaders[i].getColumnType() == ReportColumn.ColumnType.RATE)
				{
					CellFactory.createCell(row, cellNumber++, cellValue, workbook, CellStyle.CURRENCY);
				}
				else if (columnHeaders[i].getColumnType() == ReportColumn.ColumnType.DATE)
				{
					CellFactory.createCell(row, cellNumber++, cellValue, workbook, CellStyle.DATE);
				}
				else
				{
					CellFactory.createCell(row, cellNumber++, cellValue, workbook, CellStyle.NORMAL);
				}
			}
			
			i++;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see net.rrm.ehour.ui.common.component.AbstractExcelReport#getFilename()
	 */
	@Override
	protected String getFilename()
	{
		return ((String)(getExcelReportName().getObject())).toLowerCase().replace(' ', '_') + ".xls";
	}
	
	
	/**
	 * Create header containing report date
	 * @param sheet
	 */
	protected int createHeaders(int rowNumber, HSSFSheet sheet, Report report, HSSFWorkbook workbook)
	{
		HSSFRow		row;

		row = sheet.createRow(rowNumber++);
		CellFactory.createCell(row, 0, getHeaderReportName(), workbook, CellStyle.BOLD);
		sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

		row = sheet.createRow(rowNumber++);
		CellFactory.createCell(row, 0, new ResourceModel("report.dateStart"), workbook, CellStyle.BOLD);
		
		if (report.getReportRange() == null || 
				report.getReportRange().getDateStart() == null)
		{
			CellFactory.createCell(row, 1, "--", workbook, CellStyle.BOLD);
		}
		else
		{
			CellFactory.createCell(row, 1, report.getReportCriteria().getReportRange().getDateStart(), workbook, CellStyle.BOLD, CellStyle.DATE);
		}

		CellFactory.createCell(row, 3, new ResourceModel("report.dateEnd"), workbook, CellStyle.BOLD);
		
		if (report.getReportRange() == null || report.getReportRange().getDateEnd() == null)
		{
			CellFactory.createCell(row, 4, "--", workbook, CellStyle.BOLD);
		}
		else
		{
			CellFactory.createCell(row, 4, report.getReportCriteria().getReportRange().getDateEnd(), workbook, CellStyle.BOLD, CellStyle.DATE);
		}
		
		rowNumber++;
		
		return rowNumber;
	}	
}