package chalk.app.flatgen;

import android.app.*;
import android.os.*;
import java.util.*;
import java.io.*;
import android.content.*;
import android.view.*;
import android.widget.*;

public class FileChooseActivity extends ListActivity{
	public static FileFilter filter;
	public static Comparator<File> nameComp = new Comparator<File>(){
		@Override
		public int compare(File f1, File f2){
			if(f1.isDirectory() && f2.isFile()) return -1;
			if(f1.isFile() && f2.isDirectory()) return 1;
			
			return f1.getName().compareToIgnoreCase(f2.getName());
		}
	};
	
	public static File parent;
    public static File currentLocation;
    
	public static File[] currentFiles;
    
	public static ArrayList<String> def;
	
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
      	
        Bundle bundle = getIntent().getExtras();
			
        final ArrayList<String> allow = bundle.getStringArrayList("allowext");
        
        filter = new FileFilter(){
            public boolean accept(File file){
                return file.isDirectory() || 
					(allow != null && (file.getName().lastIndexOf('.') >= 0 && allow.contains(file.getName().substring(file.getName().lastIndexOf('.') + 1).toLowerCase())));
            }
        };
        
        parent = Environment.getExternalStorageDirectory();
        currentLocation = parent;
		
		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener(){
			public void onItemClick(AdapterView<?> adap, View vi, int pos, long id){
				load(currentFiles[pos]);
			}
		});
		
		update();
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event){
		if(event.getKeyCode() != KeyEvent.KEYCODE_BACK) return false;
		
		if(currentLocation.getAbsolutePath().equals(parent.getAbsolutePath())){
			setResult(RESULT_CANCELED);
			finish();
		}
		else {
			currentLocation = currentLocation.getParentFile();
			update();
		}
		
		return true;
	}
	
    public void update(){
        currentFiles = currentLocation.listFiles(filter);
		setTitle(currentLocation.getAbsolutePath());
		
		Arrays.sort(currentFiles, nameComp);
		
		getListView().setAdapter(new FileList(this, currentFiles));
    }
	
	public void load(File newFile){
		if(newFile.isDirectory()){
			currentLocation = newFile;
			update();
			return;
		}
		
		Intent intent = getIntent();
		intent.putExtra("selected", newFile.getAbsolutePath());
		
		setResult(RESULT_OK, intent);
		finish();
	}
}

class FileList extends ArrayAdapter<File>{
	public final Activity ctx;
	public File[] files;
	
	public FileList(Activity context, File[] files) {
		super(context, R.layout.filechooser_item, files);
		this.ctx = context;
		this.files = files;
	}
	
	@Override
	public View getView(int pos, View v, ViewGroup p){	
		View view = View.inflate(ctx, R.layout.filechooser_item, null);

		((TextView) view.findViewById(R.id.filechooser_text))
			.setText(files[pos].getName());

		((ImageView) view.findViewById(R.id.filechooser_image))
			.setImageResource(files[pos].isDirectory() ? R.drawable.filechooser_folder : R.drawable.filechooser_file);

		return view;
	}
}
