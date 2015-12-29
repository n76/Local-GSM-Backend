package org.fitchfamily.android.gsmlocation.ui.settings.mcc;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.TextChange;
import org.androidannotations.annotations.ViewById;
import org.fitchfamily.android.gsmlocation.R;
import org.fitchfamily.android.gsmlocation.Settings;
import org.fitchfamily.android.gsmlocation.data.MobileCountryCodes;
import org.fitchfamily.android.gsmlocation.util.LocaleUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@EActivity(R.layout.activity_area_list)
public class AreaListActivity extends AppCompatActivity implements AreaDialogFragment.Listener {
    @ViewById
    protected Toolbar toolbar;

    @ViewById
    protected RecyclerView recycler;

    @ViewById
    protected TextView search;

    private AreaAdapter adapter = new AreaAdapter();

    private Set<Integer> mccSet;

    private List<Area> areas;

    @AfterViews
    protected void init() {
        toolbar.setTitle(R.string.activity_area_list_title);
        toolbar.setNavigationIcon(
                new IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_check)
                        .actionBar()
                        .colorRes(R.color.md_white_1000)
        );
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);
        adapter.setListener(new AreaAdapter.Listener() {
            @Override
            public boolean onLongClick(Area area) {
                if (area.mmcs().size() > 1 && !TextUtils.isEmpty(area.code())) {
                    AreaDialogFragment_.builder()
                            .code(area.code())
                            .numbers(new TreeSet<>(area.mmcs()))
                            .build()
                            .show(getSupportFragmentManager());

                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public void onClick(Area area) {
                if (area.status() == Area.Status.mixed) {
                    onLongClick(area);
                } else {
                    setMccEnabled(area.mmcs(), area.status() == Area.Status.disabled);
                }
            }
        });
    }

    @TextChange(R.id.search)
    @AfterViews
    protected void updateAdapterContent() {
        final String term = search.getText().toString().toLowerCase();
        final List<Area> filtered = new ArrayList<>();

        for (Area area : getAreas()) {
            if (area.label().toLowerCase().contains(term)) {
                filtered.add(area);
            }
        }

        adapter.setAreas(filtered);
    }

    @Override
    public void setMccEnabled(int number, boolean enabled) {
        if (enabled) {
            getMccSet().add(number);
        } else {
            getMccSet().remove(number);
        }

        updateAreasStatus();
    }

    private void setMccEnabled(Set<Integer> numbers, boolean enabled) {
        for (Integer number : numbers) {
            if (enabled) {
                getMccSet().add(number);
            } else {
                getMccSet().remove(number);
            }
        }

        updateAreasStatus();
    }

    private void updateAreasStatus() {
        for (Area area : getAreas()) {
            updateStatus(area);
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean isMccEnabled(int number) {
        return getMccSet().contains(number);
    }

    private Set<Integer> getMccSet() {
        if (mccSet == null) {
            mccSet = new HashSet<>(Settings.with(this).mccFilterSet());
        }

        return mccSet;
    }

    private List<Area> getAreas() {
        if (areas == null) {
            areas = new ArrayList<>();
            Set<Integer> unknownNumbers = new TreeSet<>(getMccSet());   // used to find unassigned numbers

            for (Map.Entry<String, Set<Integer>> entry : MobileCountryCodes.with(this).getAreas().entrySet()) {
                final String code = entry.getKey();
                final String label = LocaleUtil.getCountryName(code);
                final Set<Integer> numbers = entry.getValue();

                areas.add(new Area(code, label, calculateStatusForAreaWithNumbers(numbers), numbers));

                for (int number : numbers) {
                    unknownNumbers.remove(number);
                }
            }

            Collections.sort(areas);

            for (int number : unknownNumbers) {
                final String label = getString(R.string.fragment_area_list_unknown, number);
                final Set<Integer> numbers = new HashSet<>();
                numbers.add(number);
                areas.add(new Area(null, label, Area.Status.enabled, numbers));
            }
        }

        return areas;
    }

    private void updateStatus(Area area) {
        updateStatus(area, calculateStatusForAreaWithNumbers(area.mmcs()));
    }

    private void updateStatus(Area area, Area.Status status) {
        area.status(status);
    }

    private Area.Status calculateStatusForAreaWithNumbers(Set<Integer> numbers) {
        int found = 0;

        for (int number : numbers) {
            found += mccSet.contains(number) ? 1 : 0;
        }

        if (found == 0) {
            return Area.Status.disabled;
        } else if (found == numbers.size()) {
            return Area.Status.enabled;
        } else {
            return Area.Status.mixed;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Settings.with(this).mccFilterSet(getMccSet());
    }

    @Override
    public void onBackPressed() {
        if(search != null && !TextUtils.isEmpty(search.getText())) {
            search.setText(null);
        } else {
            super.onBackPressed();
        }
    }
}
