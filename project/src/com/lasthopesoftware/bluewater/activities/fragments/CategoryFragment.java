package com.lasthopesoftware.bluewater.activities.fragments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.j256.ormlite.logger.LoggerFactory;
import com.lasthopesoftware.bluewater.R;
import com.lasthopesoftware.bluewater.activities.ViewFiles;
import com.lasthopesoftware.bluewater.activities.WaitForConnection;
import com.lasthopesoftware.bluewater.activities.adapters.PlaylistAdapter;
import com.lasthopesoftware.bluewater.activities.adapters.views.BrowseItemMenu;
import com.lasthopesoftware.bluewater.activities.common.LongClickFlipListener;
import com.lasthopesoftware.bluewater.activities.listeners.ClickPlaylistListener;
import com.lasthopesoftware.bluewater.data.service.access.IDataTask.OnCompleteListener;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection;
import com.lasthopesoftware.bluewater.data.service.helpers.connection.PollConnection.OnConnectionRegainedListener;
import com.lasthopesoftware.bluewater.data.service.objects.IItem;
import com.lasthopesoftware.bluewater.data.service.objects.Item;
import com.lasthopesoftware.bluewater.data.service.objects.Playlist;
import com.lasthopesoftware.bluewater.data.service.objects.Playlists;
import com.lasthopesoftware.bluewater.data.sqlite.access.LibrarySession;
import com.lasthopesoftware.threading.ISimpleTask;
import com.lasthopesoftware.threading.SimpleTaskState;

public class CategoryFragment extends Fragment {
	private IItem<?> mCategory;
	private ListView listView;
	private ProgressBar pbLoading;
	private RelativeLayout mLayout;
	
	private Intent mWaitForConnection;
	
    public static final String ARG_CATEGORY_POSITION = "category_position";
    public static final String IS_PLAYLIST = "Playlist";
	
    public CategoryFragment() {
    	super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	mLayout = new RelativeLayout(getActivity());
    	mLayout.setLayoutParams(new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    	
    	mWaitForConnection = new Intent(getActivity(), WaitForConnection.class);
    	
    	pbLoading = new ProgressBar(mLayout.getContext(), null, android.R.attr.progressBarStyleLarge);
    	RelativeLayout.LayoutParams pbParams = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    	pbParams.addRule(RelativeLayout.CENTER_IN_PARENT);
    	pbLoading.setLayoutParams(pbParams);
    	mLayout.addView(pbLoading);
    	
    	LibrarySession.JrFs.getVisibleViewsAsync(new ISimpleTask.OnCompleteListener<String, Void, ArrayList<IItem<?>>>() {
			
			@Override
			public void onComplete(ISimpleTask<String, Void, ArrayList<IItem<?>>> owner, ArrayList<IItem<?>> result) {
				if (owner.getState() == SimpleTaskState.ERROR) {
					for (Exception exception : owner.getExceptions()) {
						if (!(exception instanceof IOException)) continue;
						
						final ISimpleTask.OnCompleteListener<String, Void, ArrayList<IItem<?>>> _this = this;
						PollConnection.Instance.get(getActivity()).addOnConnectionRegainedListener(new OnConnectionRegainedListener() {
							
							@Override
							public void onConnectionRegained() {
								LibrarySession.JrFs.getVisibleViewsAsync(_this);
							}
						});
						PollConnection.Instance.get(getActivity()).startPolling();
						getActivity().startActivity(mWaitForConnection);
						return;
					}
					return;
				}
				
				if (result == null) return;
				mCategory = result.get(getArguments().getInt(ARG_CATEGORY_POSITION));
				BuildView();
			}
		});
    	
        return mLayout;
    }
    
    private void BuildView() {
    	if (mCategory instanceof Playlists) {
    		listView = new ListView(getActivity());
    		listView.setVisibility(View.INVISIBLE);
    		OnCompleteListener<List<Playlist>> onPlaylistCompleteListener = new OnCompleteListener<List<Playlist>>() {
				
				@Override
				public void onComplete(ISimpleTask<String, Void, List<Playlist>> owner, List<Playlist> result) {
					if (owner.getState() == SimpleTaskState.ERROR) {
						for (Exception exception : owner.getExceptions()) {
							if (!(exception instanceof IOException)) continue;
							
							PollConnection.Instance.get(getActivity()).addOnConnectionRegainedListener(new OnConnectionRegainedListener() {
								
								@Override
								public void onConnectionRegained() {
									((Playlists) mCategory).getSubItemsAsync();
								}
							});
							PollConnection.Instance.get(getActivity()).startPolling();
							getActivity().startActivity(mWaitForConnection);
							break;
						}
						return;
					}
					
					if (result == null) return;
					
					listView.setOnItemClickListener(new ClickPlaylistListener(getActivity(), (ArrayList<Playlist>) result));
					listView.setOnItemLongClickListener(new LongClickFlipListener());
		    		listView.setAdapter(new PlaylistAdapter(getActivity(), R.id.tvStandard, result));
		    		pbLoading.setVisibility(View.INVISIBLE);
		    		listView.setVisibility(View.VISIBLE);					
				}
			};
			((Playlists) mCategory).setOnItemsCompleteListener(onPlaylistCompleteListener);
			((Playlists) mCategory).getSubItemsAsync();
    	} else {
	    	listView = new ExpandableListView(getActivity());
	    	listView.setVisibility(View.INVISIBLE);
	    	
	    	OnCompleteListener<List<Item>> onItemCompleteListener = new OnCompleteListener<List<Item>>() {

				@Override
				public void onComplete(ISimpleTask<String, Void, List<Item>> owner, List<Item> result) {
					if (owner.getState() == SimpleTaskState.ERROR) {
						for (Exception exception : owner.getExceptions()) {
							if (!(exception instanceof IOException)) continue;
							
							PollConnection.Instance.get(getActivity()).addOnConnectionRegainedListener(new OnConnectionRegainedListener() {
								
								@Override
								public void onConnectionRegained() {
									((Item)mCategory).getSubItemsAsync();
								}
							});
							PollConnection.Instance.get(getActivity()).startPolling();
							getActivity().startActivity(mWaitForConnection);
							break;
						}
						return;
					}
					
					if (result == null) return;
					
					((ExpandableListView)listView).setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
						
						@Override
						public boolean onGroupClick(ExpandableListView parent, View v,
								int groupPosition, long id) {
							final Item selection = (Item)parent.getExpandableListAdapter().getGroup(groupPosition);
							try {
								if (selection.getSubItems().size() > 0) return false;
							} catch (IOException e) {
								LoggerFactory.getLogger(CategoryFragment.class).warn(e.getMessage(), e);
								return true;
							}
							final Intent intent = new Intent(parent.getContext(), ViewFiles.class);
				    		intent.setAction(ViewFiles.VIEW_ITEM_FILES);
				    		intent.putExtra(ViewFiles.KEY, selection.getKey());
				    		intent.putExtra(ViewFiles.VALUE, selection.getValue());
				    		startActivity(intent);
				    		return true;
						}
					});
			    	((ExpandableListView)listView).setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			    	    @Override
			    	    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {        	    	
			    	    	final Item selection = (Item)parent.getExpandableListAdapter().getChild(groupPosition, childPosition);
				    		final Intent intent = new Intent(parent.getContext(), ViewFiles.class);
				    		intent.setAction(ViewFiles.VIEW_ITEM_FILES);
				    		intent.putExtra(ViewFiles.KEY, selection.getKey());
				    		intent.putExtra(ViewFiles.VALUE, selection.getValue());
				    		startActivity(intent);
			    	        return true;
			    	    }
				    });
			    	listView.setOnItemLongClickListener(new LongClickFlipListener());
			    	
			    	((ExpandableListView)listView).setAdapter(new ExpandableItemListAdapter((ArrayList<Item>)result));
			    	pbLoading.setVisibility(View.INVISIBLE);
		    		listView.setVisibility(View.VISIBLE);
				}
			};
			((Item)mCategory).setOnItemsCompleteListener(onItemCompleteListener);
			((Item)mCategory).getSubItemsAsync();
    	}
    	mLayout.addView(listView);
    }
    
    public static class ExpandableItemListAdapter extends BaseExpandableListAdapter {
    	private final ArrayList<Item> mCategoryItems;
    	
    	public ExpandableItemListAdapter(ArrayList<Item> categoryItems) {
    		mCategoryItems = categoryItems;
    	}
    	
		@Override
		public Object getChild(int groupPosition, int childPosition) {
			try {
				return mCategoryItems.get(groupPosition).getSubItems().get(childPosition);
			} catch (IOException e) {
				 LoggerFactory.getLogger(CategoryFragment.class).warn(e.getMessage(), e);
				 return null;
			}
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			try {
				return ((Item)mCategoryItems.get(groupPosition).getSubItems().get(childPosition)).getKey();
			} catch (IOException e) {
				LoggerFactory.getLogger(CategoryFragment.class).warn(e.getMessage(), e);
				return 0;
			}
		}
		
		@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			try {
				return BrowseItemMenu.getView(((Item)mCategoryItems.get(groupPosition).getSubItems().get(childPosition)), convertView, parent);
			} catch (IOException e) {
				LoggerFactory.getLogger(CategoryFragment.class).warn(e.getMessage(), e);
				return null;
			}
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			try {
				return mCategoryItems.get(groupPosition).getSubItems().size();
			} catch (IOException e) {
				LoggerFactory.getLogger(CategoryFragment.class).warn(e.getMessage(), e);
				return 0;
			}
		}

		@Override
		public Object getGroup(int groupPosition) {
			return mCategoryItems.get(groupPosition);
		}

		@Override
		public int getGroupCount() {
			return mCategoryItems.size();
		}

		@Override
		public long getGroupId(int groupPosition) {
			return mCategoryItems.get(groupPosition).getKey();
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
			if (convertView == null) {
				final LayoutInflater inflator = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = (RelativeLayout) inflator.inflate(R.layout.layout_standard_text, parent, false);
				
				final TextView heldTextView = (TextView) convertView.findViewById(R.id.tvStandard);			
	
				heldTextView.setPadding(64, 20, 20, 20);
		        
		        convertView.setTag(heldTextView);
			}
			
			((TextView)convertView.getTag()).setText(mCategoryItems.get(groupPosition).getValue());

		    return convertView;
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
    	
    }
}