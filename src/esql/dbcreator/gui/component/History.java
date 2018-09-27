package esql.dbcreator.gui.component;

public class History
{
	private String name;
	private String summary;
	private String date;
	private boolean locked;
	
	public History ( String name, String summary, String date, boolean locked )
	{
		this.name = name;
		this.summary = summary;
		this.date = date;
	}
	
	public String getName ()
	{
		return name;
	}
	
	public String getSummary ()
	{
		return summary;
	}
	
	public String getDate ()
	{
		return date;	
	}
	
	public boolean isLocked ()
	{
		return locked;
	}
}