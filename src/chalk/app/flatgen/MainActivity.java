package chalk.app.flatgen;

import android.app.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.graphics.*;
import android.media.*;

import java.io.*;
import android.content.*;
import java.util.*;

import com.chiralcode.colorpicker.*;
import android.net.*;
import android.graphics.drawable.*;
import android.database.*;

public class MainActivity extends Activity implements Runnable {
	
	public static final File folder = new File(Environment.getExternalStorageDirectory(), "Chalk/FlatGen");
	public static Typeface defaultFont;
	
	public static final String[][] colors = {
		{"#1abc9c", "#16a085"},
		{"#2ecc71", "#27ae60"},
		{"#3498db", "#2980b9"},
		{"#e74c3c", "#c0392b"},
		{"#9b59b6", "#8e44ad"},
		{"#34495e", "#2c3e50"},
		{"#f1c40f", "#f39c12"},
		{"#ff9900", "#dd7700"},
		{"#e67e22", "#d35400"},
		{"#ecf0f1", "#bdc3c7"},
		{"#95a5a6", "#7f8c8d"},
		{"#505050", "#404040"},
	};
	
	public class StyleLevel {
		public static final int
			RECTANGLE = 0,
			SQUARE = 1,
			ROUND_SQUARE = 2,
			CIRCLE = 3;
	}
	
	public class ProgressRequest {
		public static final int
			CREATE_DIALOG = 0,
			UPDATE_TEXT_IMAGE = 1,
			UPDATE_SHADOW_PROGRESS = 2,
			MERGING_IMAGE = 3,
			WORK_DONE = 4,
			
			DISMISS_DIALOG = -2,
			SHOW_ERROR = -1;
	}
	
	public static ArrayList<String> fontExts = new ArrayList<String>();
	
	public ProgressDialog pd;
	public ImageView textPreview, shadowPreview;
	public File lastFile;
	
	public EditText textEdit, textSizeEdit, textColorEdit, backgroundColorEdit, shadowColorEdit;
	public TextView fontText;
	
	public AlertDialog pickDialog;
	
	public Handler h = new Handler(){
		@Override
		public void handleMessage(Message msg){switch(msg.what){
			
			case ProgressRequest.DISMISS_DIALOG:
				if(pd != null){
					pd.hide();
					pd.dismiss();
					pd = null;
				}
				break;
				
			case ProgressRequest.SHOW_ERROR:
				Toast.makeText(MainActivity.this, msg.obj != null ? msg.obj.toString() : msg.what + "", 0).show();
				pd.setMessage(getString(R.string.error) + " - " + (msg.obj != null ? msg.obj.toString() : msg.what + ""));
				
				sendEmptyMessageDelayed(ProgressRequest.DISMISS_DIALOG, 5000);
				break;
				
			case ProgressRequest.CREATE_DIALOG:
				pd = new ProgressDialog(MainActivity.this);
				pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				pd.setCancelable(false);
				pd.setMessage(getString(R.string.creatingText));
				pd.show();
				break;
			
			case ProgressRequest.UPDATE_TEXT_IMAGE:
				textPreview.setImageBitmap((Bitmap) msg.obj);
				shadowPreview.setImageBitmap((Bitmap) msg.obj);
				shadowPreview.setVisibility(View.VISIBLE);
					
				pd.setMessage(getString(R.string.creatingBitmap));
				pd.setMax(msg.arg1);
				pd.setProgress(0);
				break;
				
			case ProgressRequest.UPDATE_SHADOW_PROGRESS:
				pd.setMessage(getString(R.string.creatingShadow));
				pd.setProgress(msg.arg1);
				shadowPreview.setImageBitmap((Bitmap) msg.obj);
				break;
			
			case ProgressRequest.MERGING_IMAGE:
				pd.setMessage(getString(R.string.mergingImage));
				break;
			
			case ProgressRequest.WORK_DONE:
				pd.setMessage(getString(R.string.savingImage));
				
				shadowPreview.setVisibility(View.INVISIBLE);
				textPreview.setImageBitmap((Bitmap) msg.obj);
				
				try{
					((Bitmap) msg.obj).compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(lastFile));
					registerToMediaScanner(lastFile);
				}catch(Exception e){}
				
				sendEmptyMessage(ProgressRequest.DISMISS_DIALOG);
					
				Toast.makeText(MainActivity.this, String.format(getString(R.string.savedAlert), lastFile.getPath()), Toast.LENGTH_LONG).show();
				break;
		}}
	};
	
	@Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		
		fontExts.clear();
		fontExts.add("ttf");
		fontExts.add("otf");
		
		if(!folder.exists()) folder.mkdirs();
		defaultFont = Typeface.createFromAsset(getAssets(), "KlavikaBoldBold.otf");
		
		textPreview = ((ImageView) findViewById(R.id.preview));
		shadowPreview = ((ImageView) findViewById(R.id.previewshadow));

		textEdit = ((EditText) findViewById(R.id.text));
		textSizeEdit = ((EditText) findViewById(R.id.textsize));
		textColorEdit = ((EditText) findViewById(R.id.textcolor));
		backgroundColorEdit = ((EditText) findViewById(R.id.backcolor));
		shadowColorEdit = ((EditText) findViewById(R.id.shadowcolor));
		fontText = ((TextView) findViewById(R.id.fontpath));
		
		Toast.makeText(this, R.string.copyright, Toast.LENGTH_LONG).show();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		menu.add(0, 0, 0, R.string.info);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if(item.getItemId() == 0) startActivity(new Intent(this, InfomationActivity.class));
		return false;
	}
	
	public int dp(int dpi){
		return (int) Math.ceil(this.getResources().getDisplayMetrics().density * dpi);
	}
	
	public Bitmap getColorPreview(String[] color, int width, int height, Path path){
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.parseColor(color[1]));
		
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(Color.parseColor(color[0]));
		paint.setStyle(Paint.Style.FILL);
		
		canvas.drawPath(path, paint);
		return bitmap;
	}
	
	public void openPickDialog(View v){
		if(pickDialog == null){
			
			int width = dp(100);
			
			LinearLayout column = new LinearLayout(this);
			column.setOrientation(LinearLayout.VERTICAL);
			column.setGravity(Gravity.CENTER);
			column.setPadding(dp(10), dp(10), dp(10), dp(10));
			
			Path path = new Path();
			path.moveTo(0, 0);
			path.lineTo(width - 1, 0);
			path.lineTo(width - 1, width - 1);
			path.lineTo(0, 0);

			View.OnClickListener colorClicked = new View.OnClickListener(){
				public void onClick(View view){
					if(pickDialog != null && pickDialog.isShowing()) pickDialog.dismiss();
					
					backgroundColorEdit.setText(colors[view.getId()][0]);
					shadowColorEdit.setText(colors[view.getId()][1]);
				}
			};
			
			LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(-2, -2);
			param.leftMargin = param.topMargin = param.bottomMargin = param.rightMargin = dp(5);
			
			for(int i = 0; i < colors.length; i += 3){
				LinearLayout row = new LinearLayout(this);
				row.setOrientation(LinearLayout.HORIZONTAL);
				row.setGravity(Gravity.CENTER);
				
				for(int off = 0; off < 3; off++){
					ImageView img = new ImageView(this);
					img.setId(i + off);
					img.setImageBitmap(getColorPreview(colors[i + off], width, width, path));
					img.setOnClickListener(colorClicked);
					
					row.addView(img, param);
				}
				column.addView(row);
			}
			
			ScrollView scroll = new ScrollView(this);
			scroll.addView(column);
		
			pickDialog = new AlertDialog.Builder(this).setTitle(R.string.pick).setView(scroll).create();
		}
		
		pickDialog.show();
	}
	
	public void pickColor(final View v){
		int targetId = R.id.textcolor;
		
		if(v.getId() == R.id.pickbackcolor) targetId = R.id.backcolor;
		else if(v.getId() == R.id.pickshadowcolor) targetId = R.id.shadowcolor;
		
		final EditText target = (EditText) findViewById(targetId);
		
		try{
		
			new ColorPickerDialog(this, Color.parseColor(target.getText().toString()), new ColorPickerDialog.OnColorSelectedListener(){
				public void onColorSelected(int color){
					target.setText(String.format("#%02X%02X%02X", Color.red(color), Color.green(color), Color.blue(color)));
				}
			}).show();
			
		}catch(IllegalArgumentException e){
			target.setError(getString(R.string.colorParseError));
		}
	
	}
	
	public void chooseFontFile(View v){
		Intent intent = new Intent(MainActivity.this, FileChooseActivity.class);
		intent.putStringArrayListExtra("allowext", fontExts);
		
		startActivityForResult(intent, 123);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == 123 && resultCode == RESULT_OK)
			((TextView) findViewById(R.id.fontpath)).setText(data.getStringExtra("selected"));
	}
	
	public void undoLast(View v){
		if(lastFile != null && lastFile.exists()){
			lastFile.delete();
			registerToMediaScanner(lastFile);
			Toast.makeText(MainActivity.this, String.format(getString(R.string.deletedAlert), lastFile.getPath()), Toast.LENGTH_LONG).show();
		}
	}
	
	public static Bitmap invertBitmap(Bitmap src){
  		int width = src.getWidth(), height = src.getHeight();
		Bitmap bitmap = Bitmap.createBitmap(width, height, src.getConfig());
		
  		int color;
  	
	 	for(int y = 0; y < height; y++){
		for(int x = 0; x < width; x++){
			color = src.getPixel(x, y);
			bitmap.setPixel(x, y, Color.argb(Color.alpha(color), 255 - Color.red(color), 255 - Color.green(color), 255 - Color.blue(color)));
		}}
		return bitmap;
	}
	
	
	@Override
	public Bitmap gradient(Bitmap source) {

		Bitmap bitmap = Bitmap.createBitmap(source.getWidth(), source.getHeight(), source.getConfig());

		ComposeShader composeShader = new ComposeShader(
			new BitmapShader(
				source, 
				Shader.TileMode.CLAMP, Shader.TileMode.CLAMP
			), 
			new LinearGradient(
				0, 48, 0, source.getHeight() - 16,
				Color.BLACK, Color.argb(0, 0, 0, 0),
				Shader.TileMode.CLAMP
			),
			PorterDuff.Mode.DST_IN
		);

		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setShader(composeShader);

		Canvas canvas = new Canvas(bitmap);
		canvas.drawPaint(paint);
		
		//source.recycle();
		return bitmap;
	}
	
	public void onClick(View v){
		if(textEdit.getText().toString().equals("")){
			textEdit.setError(getString(R.string.missingTextError));
			return;
		}
		
		final EditText[] testfor = {textColorEdit, backgroundColorEdit, shadowColorEdit};
		
		for(EditText edit : testfor){
			try{
				Color.parseColor(edit.getText().toString());
			}catch(IllegalArgumentException e){
				edit.setError(getString(R.string.colorParseError));
				return;
			}
		}
		
		new Thread(this).start();
	}
	
	public void run(){
		try{
			//PREPARE THINGS
			h.sendEmptyMessage(ProgressRequest.CREATE_DIALOG);
			
			lastFile = new File(folder, "flatgen-" + (System.currentTimeMillis() / 1000) + ".png");
			
			final int styleLevel = ((Spinner) findViewById(R.id.imageStyleSpinner)).getSelectedItemPosition();
			final int alphaLevel = 160;
			
			final int WIDTH  = styleLevel == StyleLevel.RECTANGLE ? 1600 : 900;
			final int HEIGHT = 900;
			
			final String fontPath = fontText.getText().toString();
			final Typeface font   = fontPath.equals("") ? defaultFont : Typeface.createFromFile(fontPath);
			
			final String text       = textEdit.getText().toString();
			final String texts[]    = text.split("\\\\n");
			final String sizeString = textSizeEdit.getText().toString();
			final float  textSize   = sizeString.equals("") ? 200.0f : Float.parseFloat(sizeString);
			
			final int textColor       = Color.parseColor(textColorEdit      .getText().toString());
			final int shadowColor     = Color.parseColor(shadowColorEdit    .getText().toString());
			final int backgroundColor = Color.parseColor(backgroundColorEdit.getText().toString());
			
			final Bitmap textLayor       = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888); Canvas textCanvas       = new Canvas(textLayor);
			final Bitmap shadowLayor     = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
			final Bitmap backgroundLayor = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888); Canvas backgroundCanvas = new Canvas(backgroundLayor);
			final Bitmap mergeLayor      = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888); Canvas mergeCanvas      = new Canvas(mergeLayor);
			
			//DRAW TEXT
			
			Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			textPaint.setTextAlign(Paint.Align.CENTER);
			textPaint.setTextSize(textSize);
			textPaint.setTypeface(font);
			textPaint.setColor(textColor);
			
			final float textX = WIDTH  / 2;
			//final float textY = HEIGHT / 2 - (textPaint.descent() + textPaint.ascent()) / 2; 
			
			final float totalY = (texts.length - 1) * (textSize / 3.0f) + (texts.length * textSize);
			final float margin = textSize + (textSize / 3.0f);
			
			float currentY = (HEIGHT - totalY) / 2.0f + Math.abs(textPaint.ascent());
			
			for(int yi = 0; yi < texts.length; yi++){
				textCanvas.drawText(texts[yi], textX, currentY, textPaint);
				currentY += margin;
			}
			
			h.obtainMessage(ProgressRequest.UPDATE_TEXT_IMAGE, HEIGHT, 0, textLayor).sendToTarget();
		
			//DRAW SHADOW
			
			Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
			backgroundPaint.setStyle(Paint.Style.FILL);
			backgroundPaint.setColor(backgroundColor);
			
			if(styleLevel == StyleLevel.CIRCLE)
				backgroundCanvas.drawCircle(WIDTH / 2, HEIGHT / 2, HEIGHT / 2 - 40, backgroundPaint);
			else if(styleLevel == StyleLevel.ROUND_SQUARE)
				backgroundCanvas.drawRoundRect(new RectF(80, 80, HEIGHT - 80, HEIGHT - 80), 80, 80, backgroundPaint);
			else
				backgroundCanvas.drawColor(backgroundColor);
			
			for(int cy = 0; cy < HEIGHT; cy++){
				
				nextPixel :
				for(int cx = 0; cx < WIDTH; cx++){
					if(Color.alpha(textLayor.getPixel(cx, cy)) < alphaLevel) continue nextPixel;
					
					drawShadow :
					for(int lx = cx + 1, ly = cy + 1; lx < WIDTH && ly < HEIGHT; lx++, ly++){
						int currentColor = shadowLayor.getPixel(lx, ly);
						
						if(currentColor == shadowColor) break drawShadow;
						if(Color.alpha(backgroundLayor.getPixel(lx, ly)) < alphaLevel) break drawShadow;
						shadowLayor.setPixel(lx, ly, shadowColor);
					}
				}
				h.obtainMessage(ProgressRequest.UPDATE_SHADOW_PROGRESS, cy + 1, 0, shadowLayor).sendToTarget();
			}
			
			//MERGE ALL BITMAP
			h.sendEmptyMessage(ProgressRequest.MERGING_IMAGE);
			
			mergeCanvas.drawBitmap(backgroundLayor, 0, 0, null);
			mergeCanvas.drawBitmap(gradient(shadowLayor), 0, 0, null);
			mergeCanvas.drawBitmap(textLayor, 0, 0, null);
			
			textPaint.setColor(shadowColor);
			textPaint.setTextSize(20.0f);
			textPaint.setTextAlign(Paint.Align.LEFT);
			textPaint.setTypeface(Typeface.createFromAsset(getAssets(), "KlavikaLight-Plain.otf"));
			mergeCanvas.drawText("Chalk - FlatGen 0.4 b2", 5, height - 5, textPaint);
			
			//DONE! NOW SAVING...
			h.obtainMessage(ProgressRequest.WORK_DONE, mergeLayor).sendToTarget();
		
		} catch(Exception e){
			h.obtainMessage(ProgressRequest.SHOW_ERROR, e.getMessage()).sendToTarget();
		}
	}
	
	public void registerToMediaScanner(File newFile){
		MediaScannerConnection.scanFile(this, new String[]{newFile.getPath()}, new String[] {"image/png"}, null);
	}
}
