//Source file: d:\\roseoutput\\esql\\esql\\table\\SortableTableModel.java

package nl.errorsoft.esqlmanager.domain;

import javax.swing.table.DefaultTableModel;

public class SortableTableModel extends DefaultTableModel
{	private int [] indexes;
    private SortItem [] si;

   /**
    * @roseuid 3E05A70C0178
    */
   public SortableTableModel() 
   {
    
   }

	public Object getValueAt(int row, int col)
	{	
		getIndexes();
		int rowIndex = row;
		if(indexes != null && row < indexes.length)
		{	rowIndex = indexes[row];
		}
		return super.getValueAt(rowIndex, col);
	}

	public void setValueAt(Object value, int row, int col)
	{	int rowIndex = row;
		if(indexes != null && row < indexes.length)
		{	rowIndex = indexes[row];
		}
		super.setValueAt(value, rowIndex, col);
	}

	public void sortByColumn(int column, boolean ascend)
	{	Sort.fastQuickSort(getSortItems(column));

		int arraySz = si.length;

		if (ascend)
		{	for(int i = 0; i < arraySz; i++)
			{	indexes[i] = si[i].getIndex();
			}
		}
		else
		{	for(int i = 0; i < arraySz; i++)
			{	indexes[i] = si[arraySz - i - 1].getIndex();
			}
		}
		this.fireTableDataChanged();
	}

	public SortItem[] getSortItems(int column)
	{	getIndexes();
		si = new SortItem[indexes.length];

		for(int i = 0; i < si.length; i++)
		{	si[i] = new SortItem(indexes[i], getValueAt(i, column));
		}
		return si;
	}

	public int [] getIndexes()
	{	int n = getRowCount();
		if(indexes != null)
		{	if(indexes.length == n)
			{	return indexes;
			}
		}
		indexes = new int [n];
		for(int i = 0; i < n; i++)
		{	indexes[i] = i;
		}
		return indexes;
	}
}

class Sort
{
    public static void fastQuickSort(SortItem si[])
    { int array_size = si.length;

      QuickSort(si, 0, array_size-1);
      InsertionSort(si, 0, array_size-1);
    }

   private static void QuickSort(SortItem si[], int l, int r)
   {
        int M = 4;
        int i;
        int j;
        SortItem v;

        if ((r-l)>M)
        {
                i = (r+l)/2;

                if (compare(si[l].getObject(), si[i].getObject()) > 0) swap(si,l,i);     // Tri-Median Methode!
                if (compare(si[l].getObject(), si[r].getObject()) > 0) swap(si,l,r);
                if (compare(si[i].getObject(), si[r].getObject()) > 0) swap(si,i,r);

                j = r-1;
                swap(si,i,j);
                i = l;
                v = si[j];
                for(;;)
                {
                   while(compare(si[++i].getObject(), v.getObject()) < 0);
                   while(compare(si[--j].getObject(), v.getObject()) > 0);
                   if (j<i) break;
                   swap (si,i,j);
                }
                swap(si,i,r-1);
                QuickSort(si,l,j);
                QuickSort(si,i+1,r);
        }
    }

    private static void swap(SortItem si[], int i, int j)
    {
            SortItem T;
            T = si[i];
            si[i] = si[j];
            si[j] = T;
    }

    private static void InsertionSort(SortItem si[], int lo0, int hi0)
    {
            int i;
            int j;
            SortItem v;

            for (i=lo0+1;i<=hi0;i++)
            {
                    v = si[i];
                    j=i;

                    while ((j>lo0) && (compare(si[j-1].getObject(), v.getObject()) > 0))
                    {
                            si[j] = si[j-1];
                            j--;
                    }
                    si[j] = v;
            }
    }

	public static int compare(Object o1, Object o2)
  	{	if (o1 == null && o2 == null)
    	{	return  0;
    	}
    	else if (o1 == null)
    	{	return -1;
    	}
    	else if (o2 == null)
    	{	return  1;
    	}
    	else
    	{	TableData a = (TableData)o1;
    		TableData b = (TableData)o2;
    		
    		Object native1 = a.getNativeData();
    		Object native2 = b.getNativeData();
 
   		if(native1 instanceof Number && native2 instanceof Number )
    		{	
    			return compare((Number)native1, (Number)native2);
      	}
      	else if( native1 instanceof java.util.Date && native2 instanceof java.util.Date )
      	{	
      		return compare( (java.util.Date) native1, (java.util.Date) native2 );
      	}
      	else
      	{
      		return (o1.toString().toLowerCase()).compareTo(o2.toString().toLowerCase());
      	}
      }
  	}

   public static int compare(Number o1, Number o2)
   {	double n1 = o1.doubleValue();
    	double n2 = o2.doubleValue();
   
   	if (n1 < n2)
    	{	return -1;
    	}
    	else if (n1 > n2)
    	{	return 1;
    	}
    	else
    	{	return 0;
    	}
   }

   public static int compare(java.util.Date o1, java.util.Date o2)
   {	if( o1.before(o2) )
   	{
   		return -1;
   	}
   	else if( o1.after(o2) )
   	{	
   		return 1;
   	}
   	else
   	{
   		return 0;
   	}
   }   
}

class SortItem
{	private int index;
	private Object obj;

	public SortItem (int index, Object obj)
	{	this.index = index;
		this.obj = obj;
	}

	public int getIndex()
	{	return index;
	}

	public Object getObject()
	{	return obj;
	}
}
