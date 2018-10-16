package vendor_sc;


/**
 * A matching result that contains a PCC cost item, the pattern used to find it, and a score 0..1 indicating the
 * strength of this match.
 * 
 * @author mike
 *
 */
public class Match_FT implements Comparable<Match_FT> {


  /** Service Category ID */
  public int sc_id;

  /** Service Category abbr */
  public String sc_abbr;

  /** Service Category name */
  public String sc_name;


  @Override
  public int compareTo(Match_FT another) {
    return this.sc_name.compareTo(another.sc_name);
  }


  public void setSc_abbr(String sc_abbr) {
    this.sc_abbr = sc_abbr;
  }


  public void setSc_id(int sc_id) {
    this.sc_id = sc_id;
  }


  public void setSc_name(String sc_name) {
    this.sc_name = sc_name;
  }


  public String toString() {
    return String.format("%4d [%s] %s", sc_id, sc_abbr, sc_name);
  }
}
