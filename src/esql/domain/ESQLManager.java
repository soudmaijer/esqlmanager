package esql.domain;

import esql.control.*;
import esql.gui.*;

public class ESQLManager
{
	private Settings set;
	private ImageLoader imgldr;
	private final int LE = 0;
	private final int PRO = 1;
	private final int majorVersion	= 1;
	private String appName 				= "eSQLManager";
	private final String appVersion	= "1.0";
	private final int appBuild 		= 64;
	private final int appType			= PRO;

	public ESQLManager()
	{
		if( appType == PRO )
			appName = appName + " Pro";
		else
			appName = appName + " LE";
	}
	
	public int getMajorVersion()
	{
		return this.majorVersion;
	}

	public Settings getSettings()
	{
		if( set == null )
			set = new Settings();
		return set;
	}

	public String getAppName()
	{
		return appName;
	}

	public String getAppVersion()
	{
		return appVersion;
	}

	public int getAppBuild()
	{
		return appBuild;
	}

	public boolean isPro()
	{
		if( appType == PRO )
			return true;
		return false;
	}

	public ImageLoader getImageLoader()
	{
		if( imgldr == null )
		{
			imgldr = new ImageLoader( "../../icons/" );
			imgldr.addImage("redLight.gif", "redLight");
			imgldr.addImage("greenLight.gif", "greenLight");
			imgldr.addImage("advance.gif", "advance");
			imgldr.addImage("add_field.gif", "imgAddField");
			imgldr.addImage("cascade.gif", "imgCascade");
			imgldr.addImage("connect.gif", "imgConnect");
			imgldr.addImage("computer.gif", "pc");
			imgldr.addImage("create_table.gif", "imgCreateTable");
			imgldr.addImage("create_db.gif", "imgCreateDb");
			imgldr.addImage("db.gif", "dbimg");
			imgldr.addImage("db_select_20x20.gif", "db_select_20x20");
			imgldr.addImage("dbselect.gif", "dbimgsel");
			imgldr.addImage("decrease.gif", "decrease");
			imgldr.addImage("delete_field.gif", "imgDeleteField");
			imgldr.addImage("delete_row.gif", "imgDeleteRow");
			imgldr.addImage("disconnect.gif", "imgDisconnect");
			imgldr.addImage("do_run_query.gif", "imgDoRunQuery");
			imgldr.addImage("drop_db.gif", "imgDropDb");
			imgldr.addImage("drop_table.gif", "imgDropTable");
			imgldr.addImage("fd_select_20x20.gif", "fd_select_20x20");
			imgldr.addImage("field.gif", "fldimg");
			imgldr.addImage("fieldsel.gif", "fldimgsel");
			imgldr.addImage("ico16x16.gif", "windowIcon");
			imgldr.addImage("key.gif", "keyimg");
			imgldr.addImage("keysel.gif", "keyimgsel");
			imgldr.addImage("leaf.gif", "tbimg");
			imgldr.addImage("leafselect.gif", "tbimgsel");
			imgldr.addImage("new_row.gif", "imgNewRow");
			imgldr.addImage("rt_select_20x20.gif", "rt_select_20x20");
			imgldr.addImage("run_query.gif", "imgRunQuery");
			imgldr.addImage("save.gif", "imgSave");
			imgldr.addImage("sortdown.gif", "sortdown");
			imgldr.addImage("sortup.gif", "sortup");
			imgldr.addImage("tb_select_20x20.gif", "tb_select_20x20");
			imgldr.addImage("tile_horizontal.gif", "imgTileHorizontal");
			imgldr.addImage("tile_vertical.gif", "imgTileVertical");
			imgldr.addImage("update.gif", "imgUpdateRow");
			imgldr.addImage("users.gif", "imgUserManager");
			imgldr.addImage("last.gif", "imgLast");
			imgldr.addImage("first.gif", "imgFirst");
			imgldr.addImage("hasindex.gif", "imgHasIndex");
			imgldr.addImage("hasindexsel.gif", "imgHasIndexSel");
			imgldr.addImage("arrow.gif", "arrow");
			imgldr.addImage("leaf_new.gif", "tbnew");
			imgldr.addImage("leaf_rem.gif", "tbrem");
			imgldr.addImage("leaf_edit.gif", "tbedit");
			imgldr.addImage("des_add_db.gif", "add_database");
			imgldr.addImage("des_add_tb.gif", "add_table");
			imgldr.addImage("des_add_cm.gif", "add_comment");
			imgldr.addImage("des_prop_obj.gif", "des_properties");
			imgldr.addImage("des_open.gif", "des_open");
			imgldr.addImage("des_new.gif", "des_new");
			imgldr.addImage("des_save.gif", "des_save");
			imgldr.addImage("des_check.gif", "des_check");
			imgldr.addImage("des_check1.gif", "check_off");
			imgldr.addImage("des_check2.gif", "check_good");
			imgldr.addImage("des_check3.gif", "check_error");
			imgldr.addImage("../images/splash.gif", "esql");
		}
		return imgldr;
	}
}