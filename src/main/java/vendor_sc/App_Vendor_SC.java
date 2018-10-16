package vendor_sc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

  private static String HOST;

  final static Logger log = LogManager.getLogger(App_Vendor_SC.class);
  private static TreeMap<Integer, TreeSet<Match_FT>> matches;

  private static String dir_service;

  private static String SUFFIX;

  private static String COL_OUT;

  private static String COL_IN;


  public static void main(String[] args) {
    log.info("* ======================================================================== *");
    log.info("main");

    init();

    run();

    log.info("");
    log.info("   --- done ---");
  }


  private static void init() {

    dir_service = "data/";

    TreeMap<String, String> tm = new TreeMap<>();
    Utils.load_map(tm, dir_service + "config.txt", " *= *", 2);

    HOST = tm.get("HOST");
    SUFFIX = tm.get("SUFFIX");
    COL_IN = tm.get("COL_IN");
    COL_OUT = tm.get("COL_OUT");
  }


  static boolean run() {
    log.info("ExcelCol_map");

    ObjectMapper mapper = new ObjectMapper();

    try {
      File[] files = new File(dir_service + "test/").listFiles();

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
        boolean is_overwrite = false;

        Row row = sheet.getRow(0);
        int idx_text = Utils_XL.get_idx(row, COL_IN);
        int idx_sc_default = Utils_XL.get_idx(row, COL_OUT);

        if (idx_sc_default < 0)
          idx_sc_default = Utils_XL.get_col_labels(row).size();
        else
          // Output column exists: overwrite
          is_overwrite = true;

        // Iterate rows of input sheet
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
          row = sheet.getRow(r);
          if (row == null)
            continue;

          ttl_rows++;

          // Clear cell for existing output column
          if (is_overwrite)
            matches.put(r, null);

          // Cell values
          Cell cell = row.getCell(idx_text);
          String text = Utils_XL.get_cell_value(cell);

          log.info("");
          log.info(String.format("%4d  [%s]", (r + 1), text)); // +1: MS counts from 1, not 0

          // Get sc from matcher
          String json = send_query(text);
          log.debug("json   " + json);

          Result_FT result = mapper.readValue(json, Result_FT.class);

          // Save to map, write to same sheet after read is closed
          for (Match_FT match : result.serviceCategories)
            log.info("= " + match);

          if (!result.serviceCategories.isEmpty()) {
            num_set++;
            matches.put(r, result.serviceCategories);
          }
        }

        log.info("   --- read done ---");
        log.info("");

        // Output column for mapped SC(s)
        row = sheet.getRow(0);
        Cell cell = row.createCell(idx_sc_default);
        cell.setCellValue(COL_OUT);

        // (Over)write result column in Excel file
        for (Entry<Integer, TreeSet<Match_FT>> entry : matches.entrySet()) {
          row = sheet.getRow(entry.getKey());
          cell = row.createCell(idx_sc_default);
          if (entry.getValue() == null)
            row.removeCell(cell);
          else
            cell.setCellValue(Utils.list_to_string(entry.getValue()));
        }

        FileOutputStream fos = new FileOutputStream(file_in);
        workbook.write(fos);
        fos.close();

        // Coverage
        log.info(String.format("coverage:  %d / %d  =  %d %%", num_set, ttl_rows, (100 * num_set / ttl_rows)));
        log.info("   --- write done ---");

        if (!filename.endsWith(SUFFIX)) {
          filename = filename.substring(0, filename.lastIndexOf(".")) + SUFFIX;
          log.info("rename " + file_in.renameTo(new File(dir_service + "test/" + filename)));
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
    // log.info(" send_query [" + text + "]");
    String response = "";
    try {
      // Java URI/URL encoding has issues, so...
      text = text.replace("%", "%25"); // must come first

      text = text.replace(" ", "%20");
      text = text.replace("\"", "%22");
      text = text.replace("&", "%26");
      text = text.replace("'", "%27");
      text = text.replace(".", "%2E");
      text = text.replace("/", "%2F");
      text = text.replace(";", "%3B");
      text = text.replace("<", "%3C");
      text = text.replace("=", "%3D");
      text = text.replace(">", "%3E");
      text = text.replace("[", "%5B");
      text = text.replace("\\", "%5C");
      text = text.replace("]", "%5D");
      text = text.replace("{", "%7B");
      text = text.replace("|", "%7B");
      text = text.replace("}", "%7D");

      String query = "http://" + HOST + "/ci-mapper-api" //
          + "/freetext/" + text;


      URL url = new URL(query);
      log.info("query " + query);

      StringBuilder sb = new StringBuilder();
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"))) {
        for (String line; (line = reader.readLine()) != null;)
          sb.append(line);
      } catch (Exception e) {
        throw e;
        // log.info("********************");
        // log.info("*** SERVER ERROR ***");
        // log.info("********************");
        // return "{}"; // Originally a 404 code -- but also throws exceptions for bad query encoding
      }
      response = sb.toString();

      // Easier to read in both tests and in ci-mapper log
      if (response.contains("matches\":[]"))
        response = "{}";

    } catch (Exception e) {
      log.info("send_query", e);
    }

    return response;
  }

}
