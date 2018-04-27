package at.r0.reducedreality.activity;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;

import java.util.LinkedList;
import java.util.List;

import at.r0.reducedreality.R;

public class GlobalPreferencesActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_preferences);
        addPreferencesFromResource(R.xml.global_preferences);

        //iterate through all preferences and set their summary to their value
        List<Preference> prefs = getPreferenceList(getPreferenceScreen(),
                                                   new LinkedList<Preference>());
        for (Preference p : prefs)
        {
            if (p instanceof EditTextPreference)
            {
                String val = ((EditTextPreference) p).getText();
                p.setSummary(val);
            }
            p.setOnPreferenceChangeListener(this);
        }

        ListPreference listpref = (ListPreference)findPreference("maskgen_type");
        listpref.setEntryValues(new String[]{"mask_colour", "mask_lbp"});
        listpref.setEntries(new String[]{"Colour Key", "LBP Cascade Face Detection"});
    }

    private List<Preference> getPreferenceList(Preference p, List<Preference> list)
    {
        if (p instanceof PreferenceCategory || p instanceof PreferenceScreen)
        {
            PreferenceGroup g = (PreferenceGroup) p;
            int pCount = g.getPreferenceCount();
            for (int i = 0; i < pCount; i++)
            {
                getPreferenceList(g.getPreference(i), list);
            }
        }
        else
        {
            list.add(p);
        }
        return list;
    }

    @Override
    public boolean onPreferenceChange(Preference p, Object newValue)
    {
        if (p instanceof EditTextPreference)
        {
            p.setSummary((CharSequence) newValue);
        }
        return true;
    }
}
