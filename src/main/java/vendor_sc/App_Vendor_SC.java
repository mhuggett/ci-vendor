package vendor_sc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Adds a column of SCs to the given spreadsheet, based on a column of CIs in that spreadsheet.
 * 
 * @author mike
 */
public class App_Vendor_SC {

  private static String COL_IN;

  private static String COL_OUT;
  private static String dir_data;

  private static String HOST;

  final static Logger log = LogManager.getLogger(App_Vendor_SC.class);

  private static String LOG_LEVEL;

  private static TreeMap<Integer, String> matches;

  private static Integer RANGE;

  private static Integer START;

  private static String SUFFIX;


  private static String encode(String text) {
    
    // Remove non-alphanumerics, except & and %
    text = text.replaceAll("[^a-zA-Z0-9\\&%]", " ");
    text = text.replaceAll("  +", " ");

    // Encode remaining non-alphanums
    text = text.replace("%", "%25"); // must come first
    text = text.replace("&", "%26");
    text = text.replace(" ", "%20");

    // MULE BUG: endpoint returns 404 if 'bundle' appears LC anywhere in the query text
    text = text.replace("bundle", "BUNDLE");

    // text = text.replace("\"", "%22");
    // text = text.replace("'", "%27");
    // text = text.replace(".", "%2E");
    // text = text.replace("/", "%2F");
    // text = text.replace(";", "%3B");
    // text = text.replace("<", "%3C");
    // text = text.replace("=", "%3D");
    // text = text.replace(">", "%3E");
    // text = text.replace("[", "%5B");
    // text = text.replace("\\", "%5C");
    // text = text.replace("]", "%5D");
    // text = text.replace("{", "%7B");
    // text = text.replace("|", "%7B");
    // text = text.replace("}", "%7D");
    return text;
  }


  private static void init() {

    // NB. Is run as a JAR inside the TEST dir, so effective path is TEST/data/
    dir_data = "data/";

    TreeMap<String, String> tm = new TreeMap<>();
    Utils.load_map(tm, "config.txt", " *= *", 2);

    HOST = tm.get("HOST");
    SUFFIX = tm.get("SUFFIX");
    COL_IN = tm.get("COL_IN");
    COL_OUT = tm.get("COL_OUT");

    START = Integer.parseInt(tm.get("START"));
    log.info("START " + START);

    String s = tm.get("RANGE");
    if (s != null && !s.isEmpty())
      RANGE = Integer.parseInt(tm.get("RANGE"));
    log.info("RANGE " + RANGE);

    LOG_LEVEL = tm.get("LOG_LEVEL");

    if ("DEBUG".equals(LOG_LEVEL))
      Configurator.setLevel("vendor_sc", Level.DEBUG);
    else if ("TRACE".equals(LOG_LEVEL))
      Configurator.setLevel("vendor_sc", Level.TRACE);
  }


  public static void main(String[] args) {
    log.info("* ======================================================================== *");
    log.info("main");

    init();

    run();

    log.info("");
    log.info("   --- done ---");
  }


  static boolean run() {
    log.info("ExcelCol_map");

    ObjectMapper mapper = new ObjectMapper();

    try {
      File[] files = new File(dir_data).listFiles();

      for (File file_in : files) {
        String filename = file_in.getName();

        if (!Utils_XL.is_valid_file(file_in))
          continue;

        log.info("");
        log.info("* filename " + filename);
        log.info("");

        FileInputStream excelFile = new FileInputStream(file_in);
        Workbook workbook = new XSSFWorkbook(excelFile);
        Sheet sheet = workbook.getSheetAt(0);

        int num_set = 0;
        int ttl_rows = 0;
        matches = new TreeMap<>();

        Row row = sheet.getRow(0);
        int idx_text = Utils_XL.get_idx(row, COL_IN);
        int idx_sc_default = Utils_XL.get_idx(row, COL_OUT);

        if (idx_sc_default < 0)
          idx_sc_default = Utils_XL.get_col_labels(row).size();
        log.info("idx_sc_default " + idx_sc_default);

        // Iterate rows of input sheet
        for (int r = START - 1; r <= sheet.getLastRowNum(); r++) {
          if (RANGE != null && r >= START - 1 + RANGE)
            break;

          row = sheet.getRow(r);
          if (row == null)
            continue;

          ttl_rows++;

          // Clear all cells for overwrite
          matches.remove(r);
          Cell cell = row.getCell(idx_sc_default);
          if (cell != null)
            row.removeCell(cell);

          // Cell values
          cell = row.getCell(idx_text);
          String text = Utils_XL.get_cell_value(cell);

          log.info("");
          log.info(String.format("r_%4d  [%s]", (r + 1), text)); // +1: MS counts from 1, not 0

          // Get sc from matcher
          String json = send_query(text);
          log.debug("json   " + json);

          // Runtime error
          if (json.startsWith("WARN")) {
            matches.put(r, json);
            continue;
          }

          // Found Service Categories
          Result_FT result = mapper.readValue(json, Result_FT.class);
          if (!result.serviceCategories.isEmpty()) {
            num_set++;
            String scs = Utils.list_to_string(result.serviceCategories);
            log.debug("= " + scs);
            matches.put(r, scs);
          }
        }

        log.info("");
        log.info("   --- read done ---");
        log.info("");

        // Output column for mapped SC(s)
        row = sheet.getRow(0);
        Cell cell = row.createCell(idx_sc_default);
        cell.setCellValue(COL_OUT);

        // (Over)write result column in Excel file
        for (Entry<Integer, String> entry : matches.entrySet()) {
          row = sheet.getRow(entry.getKey());
          cell = row.createCell(idx_sc_default);
          cell.setCellValue(entry.getValue());
        }

        FileOutputStream fos = new FileOutputStream(file_in);
        workbook.write(fos);
        fos.close();

        // Coverage
        log.info(String.format("coverage:  %d / %d  =  %d %%", num_set, ttl_rows, (100 * num_set / ttl_rows)));
        log.info("   --- write done ---");

        if (!filename.endsWith(SUFFIX)) {
          filename = filename.substring(0, filename.lastIndexOf(".")) + SUFFIX;
          log.info("rename " + file_in.renameTo(new File(dir_data + filename)));
        }
        workbook.close();
      }

    } catch (Exception e) {
      log.info(e.getMessage(), e);
      return false;
    }
    log.info("");
    return true;
  }


  private static String send_query(String text) {
    String response = "";

    if ("TRACE".equals(LOG_LEVEL))
      Utils.print_char_encodings(log, text);

    try {
      String query = "http://" + HOST + "/ci-mapper-api/freetext/" + encode(text);

      URL url = new URL(query);
      log.info("query " + query);

      StringBuilder sb = new StringBuilder();
      InputStream stream = url.openStream();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
        for (String line; (line = reader.readLine()) != null;)
          sb.append(line);
        stream.close();
        response = sb.toString();

      } catch (Exception e) {
        response = "WARN --- result stream " + e.getClass().getSimpleName();
      }

    } catch (Exception e) {
      response = "WARN --- URL query " + e.getClass().getSimpleName();
    }

    return response;
  }

}
