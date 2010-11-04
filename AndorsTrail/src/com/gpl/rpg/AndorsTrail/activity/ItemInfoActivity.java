package com.gpl.rpg.AndorsTrail.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.model.item.ItemType;
import com.gpl.rpg.AndorsTrail.view.TraitsInfoView;

public final class ItemInfoActivity extends Activity {
	
	public static int ITEMACTION_NONE = 1;
	public static int ITEMACTION_USE = 2;
	public static int ITEMACTION_EQUIP = 3;
	public static int ITEMACTION_UNEQUIP = 4;
	public static int ITEMACTION_BUY = 5;
	public static int ITEMACTION_SELL = 6;
	
	private WorldContext world;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
        this.world = app.world;
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        final Intent intent = getIntent();
        Bundle params = intent.getExtras();
        int itemTypeID = params.getInt("itemTypeID");
        final ItemType itemType = world.itemTypes.getItemType(itemTypeID);
        
        final String buttonText = params.getString("buttonText");
        boolean buttonEnabled = params.getBoolean("buttonEnabled");
        
        
        setContentView(R.layout.iteminfo);

        ImageView img = (ImageView) findViewById(R.id.iteminfo_image);
        img.setImageBitmap(world.tileStore.bitmaps[itemType.iconID]);
        TextView tv = (TextView) findViewById(R.id.iteminfo_title);
        tv.setText(itemType.name);
        tv = (TextView) findViewById(R.id.iteminfo_category);
        tv.setText(getCategoryNameRes(itemType.category));
        
        ((TraitsInfoView) findViewById(R.id.iteminfo_traits)).update(itemType.effect_combat);
        
        tv = (TextView) findViewById(R.id.iteminfo_description);
        if (itemType.effect_health != null && itemType.effect_health.max != 0) {
        	tv.setText(getResources().getString(R.string.iteminfo_effect_heal, itemType.effect_health.toMinMaxString()));
        	tv.setVisibility(View.VISIBLE);
        } else {
        	tv.setVisibility(View.GONE);
        }
        
        Button b = (Button) findViewById(R.id.iteminfo_close);
        b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				setResult(RESULT_CANCELED);
				ItemInfoActivity.this.finish();
			}
		});
        
        b = (Button) findViewById(R.id.iteminfo_action);
        if (buttonText != null && buttonText.length() > 0) {
        	b.setVisibility(View.VISIBLE);
        	b.setEnabled(buttonEnabled);
        	b.setText(buttonText);
        } else {
        	b.setVisibility(View.GONE);
        }
        
        b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Intent result = new Intent();
				result.putExtras(intent);
				setResult(RESULT_OK, result);
				ItemInfoActivity.this.finish();
			}
		});
    }

	private static int getCategoryNameRes(int itemCategory) {
		switch (itemCategory) {
		case ItemType.CATEGORY_MONEY:
			return R.string.itemcategory_money;
		case ItemType.CATEGORY_WEAPON:
			return R.string.itemcategory_weapon;
		case ItemType.CATEGORY_SHIELD:
			return R.string.itemcategory_shield;
		case ItemType.CATEGORY_WEARABLE_HEAD:
			return R.string.itemcategory_wearable_head;
		case ItemType.CATEGORY_WEARABLE_HAND:
			return R.string.itemcategory_wearable_hand;
		case ItemType.CATEGORY_WEARABLE_FEET:
			return R.string.itemcategory_wearable_feet;
		case ItemType.CATEGORY_WEARABLE_BODY:
			return R.string.itemcategory_wearable_body;
		case ItemType.CATEGORY_WEARABLE_NECK:
			return R.string.itemcategory_wearable_neck;
		case ItemType.CATEGORY_WEARABLE_RING:
			return R.string.itemcategory_wearable_ring;
		case ItemType.CATEGORY_POTION:
			return R.string.itemcategory_potion;
		default:
			return R.string.itemcategory_other;
		}
	}

}