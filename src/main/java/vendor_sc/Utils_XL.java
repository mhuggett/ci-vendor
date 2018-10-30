package vendor_sc;

import java.io.File;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;


public class Utils_XL {

  static DataFormatter dataFormatter = new DataFormatter();
  public final static Logger log = LogManager.getLogger(Utils_XL.class);


  /**
   * Reads the cell value, using last-eval if a formula. <br>
   * Returns null if empty or '--' non-val.
   * 
   * @param val
   * @return
   */
  @SuppressWarnings("deprecation")
  public static String get_cell_value(Cell cell) {
    String val = null;

    if (Cell.CELL_TYPE_FORMULA == cell.getCellType()) {
      log.trace("formula");
      switch (cell.getCachedFormulaResultType()) {
      case Cell.CELL_TYPE_NUMERIC:
        val = String.valueOf(cell.getNumericCellValue()).trim();
        break;
      case Cell.CELL_TYPE_STRING:
        val = cell.getRichStringCellValue().getString().trim();
        break;
      case Cell.CELL_TYPE_ERROR:
        val = String.valueOf(cell.getCellFormula()).trim();
        break;
      }

    } else if (Cell.CELL_TYPE_ERROR == cell.getCellType()) {
      log.trace("error");
      val = String.valueOf(cell.getErrorCellValue()).trim();

    } else {
      log.trace("other");
      val = dataFormatter.formatCellValue(cell).trim();
    }

    return val.isEmpty() || val.equals("--") // --: deliberately not assigned
        ? null //
        : val.replaceAll("'+", "''").trim();
  }


  /**
   * Given a title row, returns a list of the column labels in order of appearance.
   * 
   * @param row
   * @return
   */
  public static ArrayList<String> get_col_labels(Row row) {
    ArrayList<String> li = new ArrayList<>();
    int col = 0;
    Cell cell = row.getCell(col);
    while (cell != null) {
      String val = dataFormatter.formatCellValue(cell).trim();
      if (val.isEmpty())
        break;
      li.add(val);
      cell = row.getCell(++col);
    }
    return li;
  }


  public static int get_idx(Row row,
                            String label) {
    int idx = -1;
    for (int col = 0; col <= row.getLastCellNum(); col++) {
      Cell cell = row.getCell(col);
      if (cell == null)
        continue;
      String text = cell.getStringCellValue();
      if (text == null)
        continue;
      if (text.trim().equalsIgnoreCase(label)) {
        idx = col;
        break;
      }
    }
    return idx;
  }


  public static boolean is_valid_file(File file) {
    String filename = file.getName();
    return !filename.startsWith("~") && filename.endsWith(".xlsx");
  }
}
