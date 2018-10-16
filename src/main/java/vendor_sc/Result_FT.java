package vendor_sc;

import java.util.TreeSet;


/**
 * A Result for a given operator cost item, containing 0..N matching PCC cost items.
 * 
 * @author mike
 *
 */
public class Result_FT {

  /** The text being mapped to the Service Categories */
  public String text;

  /** List of Matches for this op_ci. Empty if no matches found. */
  public TreeSet<Match_FT> serviceCategories;


  public void setServiceCategories(TreeSet<Match_FT> serviceCategories) {
    this.serviceCategories = serviceCategories;
  }


  public void setText(String text) {
    this.text = text;
  }


  public String toString() {
    return String.format("%s", serviceCategories);
  }
}
