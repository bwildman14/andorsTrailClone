package com.gpl.rpg.AndorsTrail.activity;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import com.gpl.rpg.AndorsTrail.Dialogs;
import com.gpl.rpg.AndorsTrail.AndorsTrailApplication;
import com.gpl.rpg.AndorsTrail.R;
import com.gpl.rpg.AndorsTrail.context.WorldContext;
import com.gpl.rpg.AndorsTrail.controller.ItemController;
import com.gpl.rpg.AndorsTrail.model.actor.MonsterType;
import com.gpl.rpg.AndorsTrail.model.actor.Player;
import com.gpl.rpg.AndorsTrail.model.item.ItemContainer;
import com.gpl.rpg.AndorsTrail.model.item.ItemType;
import com.gpl.rpg.AndorsTrail.model.item.Loot;
import com.gpl.rpg.AndorsTrail.view.ShopItemContainerAdapter;
import com.gpl.rpg.AndorsTrail.view.ShopItemContainerAdapter.OnContainerItemClickedListener;

public class ShopActivity extends TabActivity implements OnContainerItemClickedListener {

	private WorldContext world;
	private Player player;

	private ListView shoplist_buy;
	private ListView shoplist_sell;
	private ItemContainer container_buy;
	private ItemContainer container_sell;
	private TextView shop_buy_gc;
	private TextView shop_sell_gc;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndorsTrailApplication app = AndorsTrailApplication.getApplicationFromActivity(this);
        this.world = app.world;
        this.player = world.model.player;
        
        Uri uri = getIntent().getData();
        String monsterTypeID = uri.getLastPathSegment().toString();
        final MonsterType npcType = world.monsterTypes.getMonsterType(Integer.parseInt(monsterTypeID));
        
        
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        final Player player = world.model.player;
        
        setContentView(R.layout.shop);
        
        final Resources res = getResources();
        
        TabHost h = getTabHost();
        h.addTab(h.newTabSpec("buy")
        		.setIndicator(res.getString(R.string.shop_buy))
        		.setContent(R.id.shop_tab1));
        h.addTab(h.newTabSpec("sell")
        		.setIndicator(res.getString(R.string.shop_sell))
        		.setContent(R.id.shop_tab2));
        h.setup();
        shop_buy_gc = (TextView) h.findViewById(R.id.shop_buy_gc);
        shop_sell_gc = (TextView) h.findViewById(R.id.shop_sell_gc);
        
        shoplist_buy = (ListView) h.findViewById(R.id.shop_buy_list);
        /*
        shoplist_buy.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				int itemTypeID = (int) id;
				ItemType itemType = world.itemTypes.getItemType(itemTypeID);
				int price = ItemController.getBuyingPrice(player, itemType);
				String text = res.getString(R.string.shop_buyitem, price);
				Dialogs.showItemInfo(ShopActivity.this, itemTypeID, ItemInfoActivity.ITEMACTION_BUY, text, ItemController.canAfford(player, price), -1);
			}
		});
		*/
        shoplist_sell = (ListView) h.findViewById(R.id.shop_sell_list);
        /*
        shoplist_sell.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				int itemTypeID = (int) id;
				ItemType itemType = world.itemTypes.getItemType(itemTypeID);
				int price = ItemController.getBuyingPrice(player, itemType);
				String text = res.getString(R.string.shop_sellitem, price);
				Dialogs.showItemInfo(ShopActivity.this, itemTypeID, ItemInfoActivity.ITEMACTION_SELL, text, true, -1);
			}
		});
		*/
        
        Loot merchantLoot = new Loot();
        npcType.dropList.createRandomLoot(merchantLoot);
        container_buy = merchantLoot.items;
        container_sell = player.inventory;
        
		shoplist_buy.setAdapter(new ShopItemContainerAdapter(
				this
				, world.tileStore
				, player
				, container_buy
				, this
				, false));
        shoplist_sell.setAdapter(new ShopItemContainerAdapter(
        		this
        		, world.tileStore
        		, player
				, container_sell
        		, this
        		, true));
        
        update();
    }

	@Override
	public void onItemActionClicked(int position, ItemType itemType, boolean isSelling) {
		if (isSelling) {
			sell(itemType);
		} else {
			buy(itemType);
		}
	}

	@Override
	public void onItemInfoClicked(int position, ItemType itemType, boolean isSelling) {
		int price;
		int resid;
		boolean enableButton = true;
		int action;
		if (isSelling) {
			resid = R.string.shop_sellitem;
			action = ItemInfoActivity.ITEMACTION_SELL;
			price = ItemController.getSellingPrice(player, itemType);
			enableButton = ItemController.maySellItem(player, itemType);
		} else {
			resid = R.string.shop_buyitem;
			action = ItemInfoActivity.ITEMACTION_BUY;
			price = ItemController.getBuyingPrice(player, itemType);
			enableButton = ItemController.canAfford(player, price);
		}
		String text = getResources().getString(resid, price);
		Dialogs.showItemInfo(ShopActivity.this, itemType.id, action, text, enableButton, -1);
	}
	
    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case MainActivity.INTENTREQUEST_ITEMINFO:
			if (resultCode != RESULT_OK) return;
			
			ItemType itemType = world.itemTypes.getItemType(data.getExtras().getInt("itemTypeID"));
			int actionType = data.getExtras().getInt("actionType");
			if (actionType == ItemInfoActivity.ITEMACTION_BUY) {
	        	buy(itemType);
	        } else  if (actionType == ItemInfoActivity.ITEMACTION_SELL) {
	        	sell(itemType);
			}
			break;
		}
	}

	private void sell(ItemType itemType) {
		ItemController.sell(player, itemType, container_buy);
		update();
	}

	private void buy(ItemType itemType) {
		ItemController.buy(player, itemType, container_buy);
		update();
	}

	private void update() {
        updateBuyItemList();
        updateSellItemList();
        updateGc();
	}

	private void updateGc() {
		String gc = getResources().getString(R.string.shop_yourgold, player.inventory.gold);
		shop_buy_gc.setText(gc);
		shop_sell_gc.setText(gc);		
	}

	private void updateBuyItemList() {
		((ShopItemContainerAdapter) shoplist_buy.getAdapter()).notifyDataSetChanged();
    }
	private void updateSellItemList() {
		((ShopItemContainerAdapter) shoplist_sell.getAdapter()).notifyDataSetChanged();
    }
}