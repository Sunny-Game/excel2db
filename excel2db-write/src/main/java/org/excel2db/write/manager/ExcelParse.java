package org.excel2db.write.manager;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFDataFormatter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.excel2db.write.util.ConfigUtil;
import org.excel2db.write.util.Constants;
import org.excel2db.write.util.ExcelColumn;
import org.excel2db.write.util.TypeEnum;

/**
 * 读取excel
 * 
 * @author zhaohui
 * 
 */
public class ExcelParse {

	private final static Logger logger = Logger.getLogger(ExcelParse.class);

	/** 数据开始行 **/
	private static int DATA_STAR_ROW = 5;
	/** 类型的行 **/
	private static int TYPE_ROW = 2;
	/** 名字的行 **/
	private static int NAME_ROW = 0;

	/** excel文件路径 **/
	private String filePath;

	/** 字段名称,字段类型,数据 **/
	private Map<String, List<String>> columnNameMap = new HashMap<String, List<String>>();
	private Map<String, List<TypeEnum>> columnTypeMap = new HashMap<String, List<TypeEnum>>();
	private Map<String, List<List<String>>> dataMap = new HashMap<String, List<List<String>>>();

	public ExcelParse(String filePath) {
		this.filePath = filePath;
	}

	/**
	 * 读取excel
	 */
	public void readExcel() {
		logger.debug("start read excel");
		Workbook book = null;
		try {
			book = WorkbookFactory.create(new FileInputStream(filePath));

			List<String> sheetStartWithList = ConfigUtil.getSheetStartWith();
			int sheetNum = book.getNumberOfSheets();
			for (int num = 0; num < sheetNum; num++) {
				Sheet sheet = book.getSheetAt(num);
				String sheetName = sheet.getSheetName();

				if (!isStartWith(sheetStartWithList, sheetName)) {
					continue;
				}

				int rows = getRowNum(book, num);
				int columns = getColumnNum(book, num);

				List<Integer> columnList = new ArrayList<Integer>();
				List<TypeEnum> typeList = new ArrayList<TypeEnum>();
				List<String> nameList = new ArrayList<String>();

				for (int column = 0; column < columns; column++) {
					String type = getValue(sheet, column, TYPE_ROW);
					TypeEnum typeEnum = TypeEnum.type(type);
					// 不存在的类型直接抛弃，主要是方便添加一些注释的列
					if (typeEnum != null) {
						typeList.add(typeEnum);
						String name = getValue(sheet, column, NAME_ROW);
						if (name.equals("")) {
							throw new RuntimeException(
									"NameNullException:sheet=" + sheetName
											+ ",column=" + toAZ(column + 1));
						}
						nameList.add(name);
						columnList.add(column);
					}
				}

				if (nameList.size() > 0 && typeList.size() > 0) {
					columnNameMap.put(sheetName, nameList);
					columnTypeMap.put(sheetName, typeList);

					List<List<String>> dataList = new ArrayList<List<String>>();
					next: for (int row = DATA_STAR_ROW; row <= rows; row++) {
						List<String> list = new ArrayList<String>();
						for (int index = 0; index < columnList.size(); index++) {
							int column = columnList.get(index);
							TypeEnum typeEnum = typeList.get(index);

							String content = getValue(sheet, column, row);
							if (index == 0 && content.equals("")) {
								break next;
							}
							String value = getInitValue(content, typeEnum);

							value = checkValue(sheetName, typeEnum, value, row,
									column);
							list.add(value);
						}
						dataList.add(list);
					}

					dataMap.put(sheetName, dataList);
				} else {
					logger.info("please check sheet[" + sheetName
							+ "],name or type is invalid.");
				}
			}
			logger.debug("end read excel");
		} catch (Exception e) {
			logger.error("readExcel error", e);
		}
	}

	private boolean isStartWith(List<String> sheetStartWithList,
			String sheetName) {
		for (String sw : sheetStartWithList) {
			if (sheetName.startsWith(sw)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 返回Excel最大行index值，实际行数要加1
	 * 
	 * @return
	 */
	public int getRowNum(Workbook book, int sheetIndex) {
		Sheet sheet = book.getSheetAt(sheetIndex);
		return sheet.getLastRowNum();
	}

	/**
	 * 返回数据的列数
	 * 
	 * @return
	 */
	public int getColumnNum(Workbook book, int sheetIndex) {
		Sheet sheet = book.getSheetAt(sheetIndex);
		Row row = sheet.getRow(0);
		if (row != null && row.getLastCellNum() > 0) {
			return row.getLastCellNum();
		}
		return 0;
	}

	/**
	 * 获取单元格中的数值
	 * 
	 * @param sheet
	 * @param column
	 * @param row
	 * @return
	 */
	private String getValue(Sheet sheet, int column, int row) {
		Row rowObj = sheet.getRow(row);
		if (rowObj == null) {
			return "";
		}
		Cell cell = rowObj.getCell(column);
		if (cell == null) {
			return "";
		}
		cell.setCellType(Cell.CELL_TYPE_STRING);
		return cell.getStringCellValue().trim();
	}

	/**
	 * 对excle中单元格数组就行检查
	 * 
	 * @param typeEnum
	 * @param value
	 * @param row
	 * @param column
	 */
	private String checkValue(String sheetName, TypeEnum typeEnum,
			String value, int row, int column) {
		String val = value;
		try {
			if (typeEnum == TypeEnum.INT) {
				HSSFDataFormatter dataFormatter = new HSSFDataFormatter();
				val = dataFormatter.formatRawCellContents(
						Double.parseDouble(value), 0, "0");
				Integer.valueOf(val);
			} else if (typeEnum == TypeEnum.FLOAT) {
				Float.valueOf(value);
			} else if (typeEnum == TypeEnum.LONG) {
				Long.valueOf(value);
			} else if (typeEnum == TypeEnum.DOUBLE) {
				Double.valueOf(value);
			} else if (typeEnum == TypeEnum.INTS) {
				String values[] = value.split(Constants.SEPARATOR);
				for (String v : values) {
					Integer.valueOf(v);
				}
			} else if (typeEnum == TypeEnum.FLOATS) {
				String values[] = value.split(Constants.SEPARATOR);
				for (String v : values) {
					Float.valueOf(v);
				}
			} else if (typeEnum == TypeEnum.LONGS) {
				String values[] = value.split(Constants.SEPARATOR);
				for (String v : values) {
					Long.valueOf(v);
				}
			} else if (typeEnum == TypeEnum.DOUBLES) {
				String values[] = value.split(Constants.SEPARATOR);
				for (String v : values) {
					Double.valueOf(v);
				}
			}
			return val;
		} catch (Exception e) {
			if (typeEnum == TypeEnum.INT || typeEnum == TypeEnum.FLOAT
					|| typeEnum == TypeEnum.DOUBLE || typeEnum == TypeEnum.LONG) {
				throw new RuntimeException("numberFormatException:sheet="
						+ sheetName + ",row=" + (row + 1) + ",column="
						+ toAZ(column + 1));
			} else {
				throw new RuntimeException("numberFormatException:sheet="
						+ sheetName + ",row=" + (row + 1) + ",column="
						+ toAZ(column + 1) + ",separator is "
						+ Constants.SEPARATOR);
			}
		}
	}

	private String toAZ(int column) {
		return ExcelColumn.excelColIndexToStr(column);
	}

	/**
	 * 获取非string类型的初始值
	 * 
	 * @param value
	 * @return
	 */
	private String getInitValue(String value, TypeEnum typeEnum) {
		if (typeEnum == TypeEnum.INTS || typeEnum == TypeEnum.FLOATS
				|| typeEnum == TypeEnum.LONGS || typeEnum == TypeEnum.STRINGS
				|| typeEnum == TypeEnum.DOUBLES) {
			return value;
		}
		if (value.equals("")) {
			return "0";
		}
		return value;
	}

	public Map<String, List<String>> getColumnNameMap() {
		return columnNameMap;
	}

	public Map<String, List<TypeEnum>> getColumnTypeMap() {
		return columnTypeMap;
	}

	public Map<String, List<List<String>>> getDataMap() {
		return dataMap;
	}

}
