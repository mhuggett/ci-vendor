package vendor_sc;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.logging.log4j.Logger;

public class Utils {

  public static void add_to_vals(TreeMap<String, TreeSet<String>> tm,
                                 String key,
                                 String val) {
    TreeSet<String> ts = tm.get(key);
    if (ts == null) {
      ts = new TreeSet<>();
      tm.put(key, ts);
    }
    ts.add(val);
  }


  /**
   * Deletes all files from the specified directory, leaves all sub-directories intact. <br>
   * Creates the dir if not already present.
   * 
   * @param dir
   */
  public static void clean_dir(File dir) {
    dir.mkdirs();

    for (File file : dir.listFiles())
      if (!file.isDirectory())
        file.delete();
  }


  /**
   * Deletes all files from the specified directory, leaves all sub-directories intact. <br>
   * Creates the dir if not already present.
   * 
   * @param dir_str
   */
  public static void clean_dir(String dir_str) {
    clean_dir(new File(dir_str));
  }


  public static void exit() {
    System.out.println("   --- exit ---");
    System.exit(1);
  }


  public static void exit(Logger log) {
    log.fatal("   --- exit ---");
    System.exit(1);
  }


  public static void exit(Logger log,
                          Exception e) {
    log.fatal("   --- exit ---", e);
    System.exit(1);
  }


  public static void exit(Logger log,
                          Exception e,
                          Object msg) {
    log.fatal(msg);
    exit(log, e);
  }


  public static void exit(Logger log,
                          Object msg) {
    log.fatal("");
    log.fatal(msg.toString());
    exit();
  }


  public static void exit(Object msg) {
    System.out.println();
    System.out.println(msg.toString());
    exit();
  }


  /**
   * Returns first N space-delimited words.
   * 
   * @param text
   * @param num_words
   * @return
   */
  public static String first_n_words(String text,
                                     int num_words) {
    int idx = 0;
    for (int i = 0; i < num_words; i++)
      idx = text.indexOf(' ', idx + 1);
    return text.substring(0, idx);
  }


  /**
   * Returns first N space-delimited words, starting from the given char sequence. <br>
   * First char of target is omitted from the result (assumes leading ' ').
   * 
   * @param msg
   * @param num_words
   * @param target
   * @return
   */
  public static String first_n_words_from(String msg,
                                          int num_words,
                                          String target) {
    // N words from 'line'
    int idx_start = msg.indexOf(target) + 1;
    int idx = idx_start;
    for (int i = 0; i < num_words; i++)
      idx = msg.indexOf(' ', idx + 1);
    return msg.substring(idx_start, idx).trim();
  }


  public static File getResourceFile(String filepath) {
    return new File(Utils.class.getResource(filepath).getFile());
  }


  /**
   * Creates a new 'inverse' TreeMap from the given TreeMap, by inverting keys and values.
   * 
   * @param treemap
   * @return
   */
  public static TreeMap<String, TreeSet<String>> invert_map(TreeMap<String, TreeSet<String>> treemap) {
    TreeMap<String, TreeSet<String>> tm = new TreeMap<>();

    for (Entry<String, TreeSet<String>> map : treemap.entrySet()) {
      String sc = map.getKey();
      for (String pcc : map.getValue()) {
        TreeSet<String> ts = tm.get(pcc);
        if (ts == null)
          ts = new TreeSet<>();
        ts.add(sc);
        tm.put(pcc, ts);
      }
    }

    return tm;
  }


  /**
   * Removes square brackets from stringified Collection.
   * 
   * @param coll
   * @return
   */
  public static String list_to_string(Collection<?> coll) {
    if (coll == null || coll.isEmpty())
      return "";

    String s = coll.toString();
    return s.substring(1, s.length() - 1);
  }


  /**
   * Read one line per item into a List. Skips comments but keeps empty lines.
   * <p>
   * A <u>'</u> at SOL leaves as-is wrt lead/trail'g spaces.
   * 
   * @param filepath
   * @return
   */
  public static List<String> load_lines(File file) {
    ArrayList<String> al = new ArrayList<>();

    try {
      BufferedReader br = new BufferedReader(new FileReader(file));
      String line = "";

      while ((line = br.readLine()) != null) {
        if (line.startsWith("'")) { // SOL ': leave as is wrt lead/trail'g spaces
          line = line.substring(1, line.length() - 1);
        } else
          line = line.trim();

        if (line.startsWith("###")) // arbitrary EOF
          break;

        // skip comments, skip empty lines at SOF
        if (!line.startsWith("#") && !(line.isEmpty() && al.isEmpty()))
          al.add(line);
      }
      br.close();

    } catch (Exception e) {
      e.printStackTrace();
    }
    return al;
  }


  /**
   * Read one line per item into a List. Skips comments but keeps empty lines.
   * <p>
   * A <u>'</u> at SOL leaves as-is wrt lead/trail'g spaces.
   * 
   * @param filepath
   * @return
   */
  public static List<String> load_lines(String filepath) {
    return load_lines(getResourceFile(filepath));
  }


  public static Map<String, String> load_map(Map<String, String> map,
                                             String filepath,
                                             String del,
                                             int num_splits) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(filepath));
      String line = "";
      while ((line = br.readLine()) != null) {
        if (line.startsWith("#") || line.isEmpty())
          continue;

        String[] bits = line.split(del, num_splits);
        if (bits.length == 2)
          map.put(bits[0], bits[1]);
        else
          map.put(bits[0], null);
      }

      br.close();

    } catch (Exception e) {
      e.printStackTrace();
    }

    return map;
  }


  /**
   * Loads a map, key on first line and value appended from subsequent lines. <br>
   * Entries are separated by one or more blank lines.
   * 
   * @param map
   * @param filepath
   * @return
   */
  public static Map<String, String> load_map(Map<String, String> map,
                                             String filepath) {
    try {
      BufferedReader br = new BufferedReader(new FileReader(filepath));
      String line = "";
      while ((line = br.readLine()) != null) {
        if (line.startsWith("#") || line.isEmpty())
          continue;

        String key = line;

        // No value in same line: append per line until blank line
        StringBuilder sb = new StringBuilder();
        String s = "";
        do {
          s = br.readLine();
          if (s == null || s.startsWith("###"))
            break;
          while (s.startsWith("#"))
            s = br.readLine();
          sb.append(s);
        } while (!s.isEmpty());
        map.put(key, sb.toString()); // TODO -- change map to <Pattern, String>
      }

      br.close();

    } catch (Exception e) {
      e.printStackTrace();
    }

    return map;
  }


  /**
   * Checks the specified path, makes dirs if missing
   * 
   * @param dirpath
   * @return
   */
  public static File mkdirs(String dirpath) {
    File dir_out = new File(dirpath);
    if (!dir_out.exists())
      dir_out.mkdirs();
    return dir_out;
  }


  /**
   * Prints the string wrapped in square brackets.
   * 
   * @param s
   */
  public static void prbln(String s) {
    s = s == null ? null : s.toString();
    System.out.println("[" + s + "]");
  }


  public static <T> void prbln(T[] arr) {
    for (T t : arr)
      System.out.print("[" + String.valueOf(t) + "] ");
    System.out.println();
  }


  public static <T> String prblns(T[] arr) {
    StringBuilder sb = new StringBuilder();
    for (T t : arr)
      sb.append("[" + String.valueOf(t) + "] ");
    return sb.toString();
  }


  /**
   * Rounds a double to the given number of places, returning a double.
   * 
   * @param value
   * @param places
   * @return
   */
  public static double round(double value,
                             int places) {
    try {
      if (places < 0)
        throw new IllegalArgumentException();

      BigDecimal bd = new BigDecimal(value);
      bd = bd.setScale(places, RoundingMode.HALF_UP);
      return bd.doubleValue();

    } catch (Exception e) {
      // // NaN exception from UAILK_11868.xlsx --- ERROR cell in Agency fee
      // System.out.println("value " + value);
      // Utils.exit("Utils.round");
      return 0.0;
    }
  }


  /**
   * As per String.split(), but omits the empty first bit if s starts with a delimiter.
   * 
   * @param s
   * @param regex
   * @return
   */
  public static String[] split(final String s,
                               final String regex) {
    return s.replaceFirst("^" + regex, "").split(regex);
  }


  /**
   * Writes a list of any type to the specified file, as strings.
   * 
   * @param list
   * @param filepath
   * @throws Exception
   */
  public static <T> void write_list(List<T> list,
                                    String filepath) throws Exception {
    BufferedWriter bw = new BufferedWriter(new FileWriter(filepath));
    for (T t : list) {
      bw.write(String.valueOf(t));
      bw.write("\n");
    }
    bw.close();
  }


  public static void print_char_encodings(Logger log,
                                    String text) {
    for (char c : text.toCharArray())
      log.info(c + "   " + Integer.toHexString(c));
  }

}
