package com.example.splitbill3;

import android.app.Activity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.Intent;
import android.graphics.Typeface;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

public class MainActivity extends Activity {

    private EditText titleInput, amountInput, peopleInput, customInput;
    private RadioGroup splitMode;
    private LinearLayout historyLayout;
    private TextView resultText;
    private SharedPreferences preferences;
    private JSONArray expenses = new JSONArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        preferences = getSharedPreferences("hisaab_data", MODE_PRIVATE);
        loadExpenses();
        buildUI();
    }

    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 32, 32, 32);
        root.setBackgroundColor(0xFFF5F6FA);
        scroll.addView(root);

        TextView heading = new TextView(this);
        heading.setText("Hisaab-Kitaab");
        heading.setTextSize(28);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(0xFF243B53);
        root.addView(heading);

        addText(root, "Split expenses easily");

        titleInput = addInput(root, "Expense name (e.g. Dinner)");
        amountInput = addInput(root, "Total amount (₹)");
        amountInput.setInputType(8194);

        peopleInput = addInput(root, "Number of people");
        peopleInput.setInputType(2);

        addText(root, "Choose split method");

        splitMode = new RadioGroup(this);
        splitMode.setOrientation(RadioGroup.VERTICAL);

        RadioButton equal = new RadioButton(this);
        equal.setText("Split equally");
        equal.setId(View.generateViewId());
        equal.setChecked(true);

        RadioButton custom = new RadioButton(this);
        custom.setText("Custom split");
        custom.setId(View.generateViewId());

        splitMode.addView(equal);
        splitMode.addView(custom);
        root.addView(splitMode);

        customInput = addInput(root,
                "Custom amounts separated by commas (e.g. 100,200,150)");
        customInput.setVisibility(View.GONE);

        splitMode.setOnCheckedChangeListener((group, checkedId) -> {
            customInput.setVisibility(
                    checkedId == custom.getId() ? View.VISIBLE : View.GONE);
        });

        Button calculate = makeButton("Calculate & Save");
        root.addView(calculate);
        calculate.setOnClickListener(v -> calculateAndSave());

        resultText = new TextView(this);
        resultText.setTextSize(17);
        resultText.setTextColor(0xFF243B53);
        resultText.setPadding(0, 20, 0, 20);
        root.addView(resultText);

        Button share = makeButton("Share on WhatsApp");
        root.addView(share);
        share.setOnClickListener(v -> shareResult());

        addText(root, "Expense History");

        historyLayout = new LinearLayout(this);
        historyLayout.setOrientation(LinearLayout.VERTICAL);
        root.addView(historyLayout);

        setContentView(scroll);
        renderHistory();
    }

    private void addText(LinearLayout parent, String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(16);
        view.setTextColor(0xFF334155);
        view.setPadding(0, 18, 0, 8);
        parent.addView(view);
    }

    private EditText addInput(LinearLayout parent, String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextSize(16);
        input.setPadding(16, 10, 16, 10);
        parent.addView(input);
        return input;
    }

    private Button makeButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        return button;
    }

    private void calculateAndSave() {
        String title = titleInput.getText().toString().trim();
        if (title.isEmpty()) title = "Shared expense";

        double total;
        int people;

        try {
            total = Double.parseDouble(
                    amountInput.getText().toString().trim());
            people = Integer.parseInt(
                    peopleInput.getText().toString().trim());

            if (total <= 0 || people <= 0 || people > 100) {
                throw new Exception();
            }
        } catch (Exception e) {
            Toast.makeText(this,
                    "Enter a valid amount and number of people",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isCustom =
                splitMode.getCheckedRadioButtonId()
                != splitMode.getChildAt(0).getId();

        StringBuilder result = new StringBuilder();
        result.append(title).append("\nTotal: ₹")
                .append(String.format(Locale.US, "%.2f", total))
                .append("\n\n");

        JSONObject expense = new JSONObject();
        JSONArray shares = new JSONArray();

        if (isCustom) {
            String[] values = customInput.getText().toString()
                    .trim().split(",");

            if (values.length != people) {
                Toast.makeText(this,
                        "Enter exactly " + people + " custom amounts",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            double sum = 0;
            try {
                for (String value : values) {
                    double share = Double.parseDouble(value.trim());
                    if (share < 0) throw new Exception();
                    shares.put(share);
                    sum += share;
                }
            } catch (Exception e) {
                Toast.makeText(this, "Invalid custom amounts",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            if (Math.abs(sum - total) > 0.011) {
                Toast.makeText(this,
                        "Custom amounts must add up to the total",
                        Toast.LENGTH_LONG).show();
                return;
            }
        } else {
            double each = Math.round(total / people * 100.0) / 100.0;
            double assigned = 0;

            for (int i = 0; i < people; i++) {
                double share = (i == people - 1)
                        ? Math.round((total - assigned) * 100.0) / 100.0
                        : each;
                shares.put(share);
                assigned += share;
            }
        }

        for (int i = 0; i < people; i++) {
            result.append("Person ").append(i + 1).append(": ₹")
                    .append(String.format(Locale.US, "%.2f",
                            shares.optDouble(i)))
                    .append("\n");
        }

        resultText.setText(result.toString());

        try {
            expense.put("title", title);
            expense.put("total", total);
            expense.put("people", people);
            expense.put("shares", shares);
            expense.put("result", result.toString());

            expenses.put(expense);
            saveExpenses();
            renderHistory();

            Toast.makeText(this, "Expense saved",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Could not save expense",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void saveExpenses() {
        preferences.edit().putString("expenses",
                expenses.toString()).apply();
    }

    private void loadExpenses() {
        try {
            expenses = new JSONArray(
                    preferences.getString("expenses", "[]"));
        } catch (Exception e) {
            expenses = new JSONArray();
        }
    }

    private void renderHistory() {
        if (historyLayout == null) return;
        historyLayout.removeAllViews();

        for (int i = expenses.length() - 1; i >= 0; i--) {
            final int index = i;
            JSONObject item = expenses.optJSONObject(i);
            if (item == null) continue;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(16, 12, 16, 12);
            card.setBackgroundColor(0xFFFFFFFF);

            TextView details = new TextView(this);
            details.setText(item.optString("result"));
            details.setTextSize(15);
            details.setTextColor(0xFF243B53);
            card.addView(details);

            Button delete = new Button(this);
            delete.setText("Delete expense");
            delete.setOnClickListener(v -> {
                JSONArray updated = new JSONArray();
                for (int j = 0; j < expenses.length(); j++) {
                    if (j != index) updated.put(expenses.opt(j));
                }
                expenses = updated;
                saveExpenses();
                renderHistory();
            });

            card.addView(delete);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(-1, -2);
            params.setMargins(0, 8, 0, 8);
            historyLayout.addView(card, params);
        }
    }

    private void shareResult() {
        String text = resultText.getText().toString();

        if (text.trim().isEmpty()) {
            Toast.makeText(this, "Calculate an expense first",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);

        try {
            startActivity(Intent.createChooser(intent, "Share expense"));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to share",
                    Toast.LENGTH_SHORT).show();
        }
    }
                         }
