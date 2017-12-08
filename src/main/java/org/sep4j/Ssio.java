package org.sep4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.sep4j.support.SepBasicTypeConverts;
import org.sep4j.support.SepReflectionHelper;

/**
 * The facade to do records saving and retrieving. Ssio = SpreadSheet
 * Input/Output
 * 
 * @author chenjianjx
 * 
 * 
 */
public class Ssio {

	/**
	 * save records to a new workbook even if there are datum errors in the
	 * records. Any datum error will lead to an empty cell.
	 * 
	 * @param headerMap
	 *            {@code <propName, headerText>, for example <"username" as field of User class, "User Name" as the spreadsheet header text>. }
	 * @param records
	 *            the records to save.
	 * @param outputStream
	 *            the output stream for the spreadsheet
	 * @param <T>
	 *            the java type of records
	 * 
	 */
	public static <T> void save(Map<String, String> headerMap,
			Collection<T> records, OutputStream outputStream) {
		saveAndGet(headerMap, records, null, outputStream, null, null, true);
	}

	/**
	 * save records to a new workbook even if there are datum errors in the
	 * records. Any datum error will lead to {@code datumErrPlaceholder} being
	 * written to the cell.
	 * 
	 * @param headerMap
	 *            {@code <propName, headerText>, for example <"username" as field of User class, "User Name" as the spreadsheet header text>. }
	 * 
	 * @param records
	 *            the records to save.
	 * @param outputStream
	 *            the output stream for the spreadsheet
	 * @param datumErrPlaceholder
	 *            if some datum is wrong, write this place holder to the cell. Can be null. 
	 * 
	 * @param <T>
	 *            the java type of records
	 */
	public static <T> void save(Map<String, String> headerMap,
			Collection<T> records, OutputStream outputStream,
			String datumErrPlaceholder) {
		saveAndGet(headerMap, records, null, outputStream, datumErrPlaceholder,
				null, true);
	}

	/**
	 * save records to a new workbook even if there are datum errors in the
	 * records. Any datum error will lead to datumErrPlaceholder being written
	 * to the cell. All the datum errors will be saved to {@code datumErrors}
	 * indicating the recordIndex of the datum
	 * 
	 * @param headerMap
	 *            {@code <propName, headerText>, for example <"username" as field of User class, "User Name" as the spreadsheet header text>. }
	 * @param records
	 *            the records to save.
	 * @param outputStream
	 *            the output stream for the spreadsheet
	 * @param datumErrPlaceholder
	 *            if some datum is wrong, write this place holder to the cell. Can be null
	 * 
	 * @param datumErrors
	 *            all data errors in the records. If you don't care, pass in null
	 * 
	 * @param <T>
	 *            the java type of records
	 */
	public static <T> void save(Map<String, String> headerMap,
			Collection<T> records, OutputStream outputStream,
			String datumErrPlaceholder, List<DatumError> datumErrors) {
		saveAndGet(headerMap, records, null, outputStream, datumErrPlaceholder,
				datumErrors, true);
	}

	/**
	 * save records to a new workbook only if there are no datum errors in the
	 * records. All the datum errors will be saved to {@code datumErrors}
	 * indicating the recordIndex of the datum
	 * 
	 * @param headerMap
	 *            {@code <propName, headerText>, for example <"username" field of User class, "User Name" as the spreadsheet header text>. }
	 * @param records
	 *            the records to save.
	 * @param outputStream
	 *            the output stream for the spreadsheet
	 * @param datumErrors
	 *            all data errors in the records. If you don't care, pass in null
	 * 
	 * @param <T>
	 *            the java type of records
	 */
	public static <T> void saveIfNoDatumError(Map<String, String> headerMap,
			Collection<T> records, OutputStream outputStream,
			List<DatumError> datumErrors) {
		saveAndGet(headerMap, records, null, outputStream, null, datumErrors,
				false);
	}

	/**
	 * Save records to a new workbook with a sheet name, and return the created
	 * workbook object. You can use this returned workbook object to append more
	 * sheets. See
	 * {@link #appendSheet(Map, Collection, Workbook, String, OutputStream, String, List, boolean)}
	 * 
	 * @param headerMap
	 *            {@code <propName, headerText>, for example <"username" as field of User class, "User Name" as the spreadsheet header text>. }
	 * @param records
	 *            the records to save.
	 * @param sheetName
	 *            the name of the sheet. Can be null
	 * @param outputStream
	 *            the output stream for the spreadsheet
	 * @param datumErrPlaceholder
	 *            if some datum is wrong, write this place holder to the cell (
	 *            {@code stillSaveIfDataError} has to true). This argument can be null.
	 * @param datumErrors
	 *            all data errors in the records. If you don't care, pass in null
	 * @param stillSaveIfDataError
	 *            if there are errors in data, should the records still be
	 *            saved?
	 * @return The created {@link XSSFWorkbook} object
	 * 
	 */
	public static <T> XSSFWorkbook saveAndGet(Map<String, String> headerMap,
			Collection<T> records, String sheetName, OutputStream outputStream,
			String datumErrPlaceholder, List<DatumError> datumErrors,
			boolean stillSaveIfDataError) {
		XSSFWorkbook wb = new XSSFWorkbook();
		saveSheet(headerMap, records, wb, sheetName, outputStream,
				datumErrPlaceholder, datumErrors, stillSaveIfDataError);
		return wb;

	}

	/**
	 * Save records to a sheet and append it to an existing workbook created by
	 * {@link #saveAndGet(Map, Collection, String, OutputStream, String, List, boolean)}
	 * 
	 * @param headerMap
	 *            {@code <propName, headerText>, for example <"username" as field of User class, "User Name" as the spreadsheet header text>. }
	 * 
	 * @param records
	 *            the records to save.
	 * @param workbook
	 *            the workbook you get from
	 *            {@link #saveAndGet(Map, Collection, String, OutputStream, String, List, boolean)}
	 * @param sheetName
	 *            the name of the sheet. Can be null
	 * @param outputStream
	 *            the output stream for the spreadsheet
	 * @param datumErrPlaceholder
	 *            if some datum is wrong, write this place holder to the cell (
	 *            {@code stillSaveIfDataError} has to true). This field can be null
	 * @param datumErrors
	 *            all data errors in the records. If you don't care, pass in null
	 * @param stillSaveIfDataError
	 *            if there are errors in data, should the records still be
	 *            saved?
	 * @return The created {@link XSSFWorkbook} object
	 * 
	 */
	public static <T> XSSFWorkbook appendSheet(Map<String, String> headerMap,
			Collection<T> records, XSSFWorkbook workbook, String sheetName,
			OutputStream outputStream, String datumErrPlaceholder,
			List<DatumError> datumErrors, boolean stillSaveIfDataError) {
		saveSheet(headerMap, records, workbook, sheetName, outputStream,
				datumErrPlaceholder, datumErrors, stillSaveIfDataError);
		return workbook;
	}

	private static <T> void saveSheet(Map<String, String> headerMap,
			Collection<T> records, XSSFWorkbook workbook, String sheetName,
			OutputStream outputStream, String datumErrPlaceholder,
			List<DatumError> datumErrors, boolean stillSaveIfDataError) {
		validateHeaderMap(headerMap);
		if (records == null) {
			records = new ArrayList<T>();
		}
		if (outputStream == null) {
			throw new IllegalArgumentException(
					"the outputStream can not be null");
		}

		Sheet sheet = workbook.createSheet(sheetName);
		createHeaders(headerMap, sheet);

		int recordIndex = 0;
		for (T record : records) {
			int rowIndex = recordIndex + 1;
			createRow(headerMap, record, recordIndex, sheet, rowIndex,
					datumErrPlaceholder, datumErrors);
			recordIndex++;
		}

		if (shouldSave(datumErrors, stillSaveIfDataError)) {
			writeWorkbook(workbook, outputStream);
		}
	}

	/**
	 * Works like {@link #parse(Map, InputStream, List, Class)} to parse the
	 * first sheet, except that it will just ignore parsing errors
	 */
	public static <T> List<T> parseIgnoringErrors(
			Map<String, String> reverseHeaderMap, InputStream inputStream,
			Class<T> recordClass) {
		try {
			return parse(reverseHeaderMap, inputStream, null, recordClass);
		} catch (InvalidFormatException | InvalidHeaderRowException e) {
			return emptyParseResultAfterException(e);
		}
	}

	/**
	 * Works like {@link #parse(Map, InputStream, List, Class)} except that it
	 * allows you select a sheet and will just ignore parsing errors
	 * 
	 * @throws IllegalArgumentException
	 *             If the sheetIndex is out of bound
	 */
	public static <T> List<T> parseIgnoringErrors(int sheetIndex,
			Map<String, String> reverseHeaderMap, InputStream inputStream,
			Class<T> recordClass) {
		try {
			return parse(sheetIndex, reverseHeaderMap, inputStream, null,
					recordClass);
		} catch (InvalidFormatException | InvalidHeaderRowException e) {
			return emptyParseResultAfterException(e);
		}
	}

	/**
	 * Works like {@link #parse(Map, InputStream, List, Class)} except that it
	 * allows you select a sheet and will just ignore parsing errors
	 * 
	 * @throws IllegalArgumentException
	 *             If the sheet with the give name doesn't exist
	 */
	public static <T> List<T> parseIgnoringErrors(String sheetName,
			Map<String, String> reverseHeaderMap, InputStream inputStream,
			Class<T> recordClass) {
		try {
			return parse(sheetName, reverseHeaderMap, inputStream, null,
					recordClass);
		} catch (InvalidFormatException | InvalidHeaderRowException e) {
			return emptyParseResultAfterException(e);
		}
	}

	/**
	 * <p>
	 * parse the first sheet of an spreadsheet to a list of beans.
	 * </p>
	 * The columns are not identified by the column indexes, but by the header
	 * rows' text of the columns specified by parameter reverseHeaderMap , i.e.
	 * you don't have to worry which column to put "username". All you need to
	 * do is to let the spreadsheet have a header column named "User Name" and
	 * associate it with "username" property in parameter reverseHeaderMap
	 * 
	 * @param reverseHeaderMap
	 *            {@code <headerText, propName>, for example <"User Name" as the spreadsheet header, "username" of User class>.}
	 * @param inputStream
	 *            the input stream of this spreadsheet
	 * @param cellErrors
	 *            the errors of data rows (not including header row) found while
	 *            being parsed. The error here can tell you which cell is wrong.
	 * @param recordClass
	 *            the class the java bean. It must have a default constructor
	 * @param <T>
	 *            the java type of records
	 * @return a list of beans
	 * @throws InvalidFormatException
	 *             the input stream doesn't represent a valid spreadsheet
	 * @throws InvalidHeaderRowException
	 *             the header row of the spreadsheet is not valid, for example,
	 *             no headerText accords to that of the reverseHeaerMap
	 */
	public static <T> List<T> parse(Map<String, String> reverseHeaderMap,
			InputStream inputStream, List<CellError> cellErrors,
			Class<T> recordClass) throws InvalidFormatException,
			InvalidHeaderRowException {
		int sheetIndex = 0;
		return parse(sheetIndex, reverseHeaderMap, inputStream, cellErrors,
				recordClass);
	}

	/**
	 * <p>
	 * parse the Nth sheet to a list of beans. It works like
	 * {@link #parse(Map, InputStream, List, Class)} except that it allows you
	 * to choose a sheet
	 * </p>
	 * 
	 * @throws IllegalArgumentException
	 *             If the sheetIndex is out of bound
	 */
	public static <T> List<T> parse(int sheetIndex,
			Map<String, String> reverseHeaderMap, InputStream inputStream,
			List<CellError> cellErrors, Class<T> recordClass)
			throws InvalidFormatException, InvalidHeaderRowException,
			IllegalArgumentException {
		Function<Workbook, Sheet> selectSheetFunction = buildSelectSheetByIndexFunction(sheetIndex);
		return doParse(selectSheetFunction, reverseHeaderMap, inputStream,
				cellErrors, recordClass);
	}

	/**
	 * <p>
	 * parse the sheet with the given name to a list of beans. It works like
	 * {@link #parse(Map, InputStream, List, Class) } except that it allows you
	 * to choose a sheet
	 * </p>
	 * 
	 * @throws IllegalArgumentException
	 *             If the sheet with the give name doesn't exist
	 */
	public static <T> List<T> parse(String sheetName,
			Map<String, String> reverseHeaderMap, InputStream inputStream,
			List<CellError> cellErrors, Class<T> recordClass)
			throws InvalidFormatException, InvalidHeaderRowException {
		Function<Workbook, Sheet> selectSheetFunction = buildSelectSheetByNameFunction(sheetName);
		return doParse(selectSheetFunction, reverseHeaderMap, inputStream,
				cellErrors, recordClass);
	}

	private static Function<Workbook, Sheet> buildSelectSheetByIndexFunction(
			int index) {
		Function<Workbook, Sheet> selectSheetFunction = new Function<Workbook, Sheet>() {
			@Override
			public Sheet apply(Workbook workbook) {
				return workbook.getSheetAt(index);
			}
		};
		return selectSheetFunction;
	}

	private static Function<Workbook, Sheet> buildSelectSheetByNameFunction(
			String name) {
		Function<Workbook, Sheet> selectSheetFunction = new Function<Workbook, Sheet>() {
			@Override
			public Sheet apply(Workbook workbook) {
				return workbook.getSheet(name);
			}
		};
		return selectSheetFunction;
	}

	private static <T> List<T> doParse(
			Function<Workbook, Sheet> selectSheetFunction,
			Map<String, String> reverseHeaderMap, InputStream inputStream,
			List<CellError> cellErrors, Class<T> recordClass)
			throws InvalidFormatException, InvalidHeaderRowException {
		validateReverseHeaderMap(reverseHeaderMap);

		validateRecordClass(recordClass);

		Workbook workbook = toWorkbook(inputStream);
		if (workbook.getNumberOfSheets() <= 0) {
			return new ArrayList<T>();
		}

		Sheet sheet = selectSheetFunction.apply(workbook);

		// key = columnIndex, value= {propName, headerText}
		Map<Short, ColumnMeta> columnMetaMap = parseHeader(reverseHeaderMap,
				sheet.getRow(0));
		if (columnMetaMap.isEmpty()) {
			throw new InvalidHeaderRowException();
		}

		// now do the data rows
		List<T> records = new ArrayList<T>();
		for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
			Row row = sheet.getRow(rowIndex);
			if (row == null) {
				continue;
			}
			T record = parseDataRow(columnMetaMap, row, rowIndex, recordClass,
					cellErrors);
			records.add(record);
		}
		return records;
	}

	private static <T> List<T> emptyParseResultAfterException(Exception e) {
		// ignore the exception
		return new ArrayList<T>();
	}

	/**
	 * the workbook has been generated. Should we write it to the outputstream?
	 */
	static boolean shouldSave(List<DatumError> datumErrors,
			boolean stillSaveIfDataError) {
		if (stillSaveIfDataError) {
			return true;
		}
		if (datumErrors == null || datumErrors.isEmpty()) {
			return true;
		} else {
			return false;
		}
	}

	static void validateRecordClass(Class<?> recordClass) {
		if (recordClass == null) {
			throw new IllegalArgumentException(
					"the recordClass can not be null");
		}
	}

	static void validateReverseHeaderMap(Map<String, String> reverseHeaderMap) {
		if (reverseHeaderMap == null || reverseHeaderMap.isEmpty()) {
			throw new IllegalArgumentException(
					"the reverseHeaderMap can not be null or empty");
		}

		int columnIndex = 0;
		for (Map.Entry<String, String> entry : reverseHeaderMap.entrySet()) {
			String headerText = entry.getKey();
			String propName = entry.getValue();
			if (StringUtils.isBlank(headerText)) {
				throw new IllegalArgumentException(
						"One header defined in the reverseHeaderMap has a blank headerText. Header Index (0-based) = "
								+ columnIndex);
			}

			if (StringUtils.isBlank(propName)) {
				throw new IllegalArgumentException(
						"One header defined in the reverseHeaderMap has a blank propName. Header Index (0-based) = "
								+ columnIndex);
			}
			columnIndex++;
		}
	}

	static void validateHeaderMap(Map<String, String> headerMap) {
		if (headerMap == null || headerMap.isEmpty()) {
			throw new IllegalArgumentException(
					"the headerMap can not be null or empty");
		}
		int columnIndex = 0;
		for (Map.Entry<String, String> entry : headerMap.entrySet()) {
			String propName = entry.getKey();
			if (StringUtils.isBlank(propName)) {
				throw new IllegalArgumentException(
						"One header has a blank propName. Header Index (0-based) = "
								+ columnIndex);
			}
			columnIndex++;
		}
	}

	static <T> void setPropertyWithCellValue(Class<T> recordClass, T record,
			String propName, Object cellStringOrDate) {
		IllegalArgumentException noSetterException = new IllegalArgumentException(
				MessageFormat
						.format("No suitable setter for property \"{0}\" with cellValue \"{1}\" ",
								propName, cellStringOrDate));
		List<Method> setters = SepReflectionHelper.findSettersByPropName(
				recordClass, propName);

		// no setter for this prop
		if (setters.isEmpty()) {
			throw noSetterException;
		}

		if (cellStringOrDate == null) {
			// in this case, try all the setters one by one
			for (Method setter : setters) {
				Class<?> propClass = setter.getParameterTypes()[0];
				if (SepBasicTypeConverts.canFromNull(propClass)) {
					SepReflectionHelper.invokeSetter(setter, record, null);
					return;
				}
			}
			throw noSetterException;
		}

		if (cellStringOrDate instanceof java.util.Date) {
			Method setter = SepReflectionHelper.findSetterByPropNameAndType(
					recordClass, propName, java.util.Date.class);
			if (setter == null) {
				throw noSetterException;
			} else {
				SepReflectionHelper.invokeSetter(setter, record,
						cellStringOrDate);
				return;
			}
		}

		// ok, we got a string
		String cellText = (String) cellStringOrDate;

		// try to find a string-type setter first
		Method stringSetter = SepReflectionHelper.findSetterByPropNameAndType(
				recordClass, propName, String.class);
		if (stringSetter != null) {
			SepReflectionHelper.invokeSetter(stringSetter, record, cellText);
			return;
		}

		// no string-type setter? do a guess!

		for (Method setter : setters) {
			Class<?> propClass = setter.getParameterTypes()[0];
			if (SepBasicTypeConverts.canFromThisString(cellText, propClass)) {
				Object propValue = SepBasicTypeConverts.fromThisString(
						cellText, propClass);
				SepReflectionHelper.invokeSetter(setter, record, propValue);
				return;
			}
		}

		throw noSetterException;
	}

	static <T> T createRecordInstance(Class<T> recordClass) {
		try {
			Constructor<T> constructor = recordClass
					.getDeclaredConstructor(new Class[0]);
			constructor.setAccessible(true);
			return constructor.newInstance(new Object[0]);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * read the cell. it only supports: boolean, numeric, date(numeric cell type
	 * + date cell format) and string.
	 * 
	 * @param cell
	 *            the cell to read
	 * @return the date if it is a date cell, or else the string value (will be
	 *         trimmed to null) . <br/>
	 * 
	 * 
	 */
	static Object readCellAsStringOrDate(Cell cell) {
		if (cell == null) {
			return null;
		}

		if (cell.getCellType() == Cell.CELL_TYPE_BLANK) {
			return null;
		}

		if (cell.getCellType() == Cell.CELL_TYPE_BOOLEAN) {
			return String.valueOf(cell.getBooleanCellValue());
		}

		if (cell.getCellType() == Cell.CELL_TYPE_ERROR) {
			return null;
		}

		if (cell.getCellType() == Cell.CELL_TYPE_FORMULA) {
			return null;
		}

		if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
			if (DateUtil.isCellDateFormatted(cell)) {
				return cell.getDateCellValue();
			} else {
				double v = cell.getNumericCellValue();
				return String.valueOf(v);
			}
		}

		if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
			String s = cell.getStringCellValue();
			return StringUtils.trimToNull(s);
		}
		return null;

	}

	private static <T> T parseDataRow(Map<Short, ColumnMeta> columnMetaMap,
			Row row, int rowIndex, Class<T> recordClass,
			List<CellError> cellErrors) {
		T record = createRecordInstance(recordClass);

		for (short columnIndex = 0; columnIndex < row.getLastCellNum(); columnIndex++) {
			ColumnMeta columnMeta = columnMetaMap.get(columnIndex);
			if (columnMeta == null || columnMeta.propName == null) {
				continue;
			}
			String propName = columnMeta.propName;
			Cell cell = row.getCell(columnIndex);
			Object cellStringOrDate = readCellAsStringOrDate(cell);
			try {
				setPropertyWithCellValue(recordClass, record, propName,
						cellStringOrDate);
			} catch (Exception e) {
				if (cellErrors != null) {
					CellError ce = new CellError();
					ce.setColumnIndex(columnIndex);
					ce.setHeaderText(columnMeta.headerText);
					ce.setPropName(propName);
					ce.setRowIndex(rowIndex);
					ce.setCause(e);
					cellErrors.add(ce);
				}
			}
		}

		return record;
	}

	/**
	 * meta info about a column
	 * 
	 * 
	 */
	private static class ColumnMeta {
		public String propName;
		public String headerText;

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this,
					ToStringStyle.SHORT_PREFIX_STYLE);
		}
	}

	/**
	 * to get <columnIndex, column info>
	 */
	private static Map<Short, ColumnMeta> parseHeader(
			Map<String, String> reverseHeaderMap, Row row) {
		Map<Short, ColumnMeta> columnMetaMap = new LinkedHashMap<Short, ColumnMeta>();

		// note that row.getLastCellNum() is one-based
		for (short columnIndex = 0; columnIndex < row.getLastCellNum(); columnIndex++) {
			Cell cell = row.getCell(columnIndex);
			Object headerObj = readCellAsStringOrDate(cell);
			String headerText = headerObj == null ? "" : headerObj.toString();
			if (headerText == null) {
				continue;
			}
			String propName = reverseHeaderMap.get(headerText);
			if (propName == null) {
				continue;
			}

			ColumnMeta cm = new ColumnMeta();
			cm.headerText = headerText;
			cm.propName = propName;
			columnMetaMap.put(columnIndex, cm);
		}
		return columnMetaMap;
	}

	private static Row createHeaders(Map<String, String> headerMap, Sheet sheet) {
		CellStyle style = sheet.getWorkbook().createCellStyle();
		style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
		style.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style.setBorderBottom(HSSFCellStyle.BORDER_THIN);
		style.setBorderTop(HSSFCellStyle.BORDER_THIN);
		style.setBorderRight(HSSFCellStyle.BORDER_THIN);
		style.setBorderLeft(HSSFCellStyle.BORDER_THIN);

		Row header = sheet.createRow(0);
		int columnIndex = 0;
		for (Map.Entry<String, String> entry : headerMap.entrySet()) {
			String headerText = StringUtils.defaultString(entry.getValue());
			Cell cell = createCell(header, columnIndex);
			cell.setCellValue(headerText);
			cell.setCellStyle(style);
			sheet.autoSizeColumn(columnIndex);
			columnIndex++;
		}

		return header;
	}

	private static <T> Row createRow(Map<String, String> headerMap, T record,
			int recordIndex, Sheet sheet, int rowIndex,
			String datumErrPlaceholder, List<DatumError> datumErrors) {
		Row row = sheet.createRow(rowIndex);
		int columnIndex = 0;

		for (Map.Entry<String, String> entry : headerMap.entrySet()) {
			boolean datumErr = false;
			String propName = entry.getKey();
			Object propValue = null;
			try {
				propValue = SepReflectionHelper.getProperty(record, propName);
			} catch (Exception e) {
				if (datumErrors != null) {
					DatumError de = new DatumError();
					de.setPropName(propName);
					de.setRecordIndex(recordIndex);
					de.setCause(e);
					datumErrors.add(de);
				}
				datumErr = true;
				propValue = datumErrPlaceholder;
			}
			String propValueText = (propValue == null ? null : propValue
					.toString());
			Cell cell = createCell(row, columnIndex);
			cell.setCellValue(StringUtils.defaultString(propValueText));

			if (datumErr) {
				CellStyle errStyle = sheet.getWorkbook().createCellStyle();
				errStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
				errStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
				cell.setCellStyle(errStyle);
			}

			columnIndex++;
		}

		return row;
	}

	private static Cell createCell(Row row, int columnIndex) {
		Cell cell = row.createCell(columnIndex);
		return cell;
	}

	private static void writeWorkbook(Workbook workbook,
			OutputStream outputStream) {
		try {
			workbook.write(outputStream);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static Workbook toWorkbook(InputStream inputStream)
			throws InvalidFormatException {
		try {
			return WorkbookFactory.create(inputStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}