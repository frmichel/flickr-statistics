package fm.flickr.stat.param;

import fm.flickr.api.wrapper.service.param.GroupItem;

/**
 * Store info of a group (pool of photos), in addition to a counter of occurrences of that group,
 * or said differently, the number of explored photos in that group over a given time slot
 * which is used when computing statistics on groups 
 * 
 * @author fmichel
 */
public class GroupItemStat extends GroupItem implements Comparable<GroupItemStat>
{
	private static final long serialVersionUID = -645219734821249499L;

	public static final String FIELD_SEPARATOR = " #_SEP_# ";

	private int nbOccurences;

	public GroupItemStat() {
		super();
		nbOccurences = 0;
	}

	public GroupItemStat(String groupId, String groupName, int nbOccurences) {
		this.groupId = groupId;
		this.groupName = groupName;
		this.nbOccurences = nbOccurences;
	}

	public GroupItemStat(GroupItem groupItem, int nbOccurences) {
		this.groupId = groupItem.getGroupId();
		this.groupName = groupItem.getGroupName();
		this.nbOccurences = nbOccurences;
	}

	public GroupItemStat(GroupItem groupItem) {
		this.groupId = groupItem.getGroupId();
		this.groupName = groupItem.getGroupName();
		this.nbOccurences = 0;
	}

	public int getNbOccurences() {
		return nbOccurences;
	}

	public void setNbOccurences(int nbOccurences) {
		this.nbOccurences = nbOccurences;
	}

	public void incNbOccurences() {
		this.nbOccurences++;
	}

	public void incNbOccurences(int inc) {
		this.nbOccurences += inc;
	}

	public String toStringShort() {
		return groupId + " (" + groupName + ")";
	}

	public String toString() {
		return groupId + ": " + nbOccurences + " occurences (" + groupName + "): ";
	}

	public String toStringL() {
		return groupName + " (" + nbOccurences + " occurences)";
	}

	public String toStringLL() {
		return groupName + " (" + nbOccurences + " occurences, " + nbPhotos + " photos, " + nbMembers + " members)";
	}

	public String toFile() {
		return groupId + FIELD_SEPARATOR + groupName + FIELD_SEPARATOR + nbOccurences;
	}

	@Override
	public int compareTo(GroupItemStat group) {

		if (this.nbOccurences == group.nbOccurences)
			return 0;
		if (this.nbOccurences < group.nbOccurences)
			return 1;
		else
			return -1;
	}
}
