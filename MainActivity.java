package com.example.splitbill3;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private final List<JSONObject> bills = new ArrayList<>();
    private EditText[] nameInputs = new EditText[3];
    private EditText descInput, amountInput;
    private EditText[] customInputs = new EditText[3];
    private Spinner payerSpinner, splitModeSpinner;
    private CheckBox[] participantChecks = new CheckBox[3];
    private LinearLayout root, summaryBox, historyBox;
    private TextView totalView, averageView, settlementView;
    private final String[] defaultNames = {"You", "Friend 2", "Friend 3"};
    private int nextId = 1;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("split_bill_data", MODE_PRIVATE);
        loadBills();
        buildUi();
        refresh();
    }

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String money(double value) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(value);
    }

    private String[] names() {
        String[] result = new String[3];
        for (int i=0;i<3;i++) {
            String s = nameInputs[i] == null ? defaultNames[i] : nameInputs[i].getText().toString().trim();
            result[i] = s.isEmpty() ? defaultNames[i] : s;
        }
        return result;
    }

    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(0xFF202124);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(0, dp(4), 0, dp(4));
        return t;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private void addSpace(LinearLayout parent, int heightDp) {
        View v = new View(this);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(heightDp)));
    }

    private EditText edit(String hint, int inputType) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setTextSize(15);
        e.setHint(hint);
        e.setInputType(inputType);
        e.setPadding(dp(10), dp(8), dp(10), dp(8));
        return e;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        return b;
    }

    private void section(LinearLayout parent, String title) {
        TextView t = text(title, 18, true);
        t.setPadding(0, dp(12), 0, dp(8));
        parent.addView(t);
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        root = vertical();
        root.setPadding(dp(18), dp(14), dp(18), dp(24));
        scroll.addView(root);
        setContentView(scroll);

        root.addView(text("Split Bill 💸", 27, true));
        root.addView(text("Three friends · clear settlements · saved on this phone", 13, false));
        section(root, "1. Friends");
        LinearLayout namesRow = horizontal();
        for (int i=0;i<3;i++) {
            LinearLayout col = vertical();
            col.setPadding(dp(3), 0, dp(3), 0);
            col.addView(text("Friend " + (i+1), 12, false));
            nameInputs[i] = edit(defaultNames[i], InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
            nameInputs[i].setText(defaultNames[i]);
            nameInputs[i].addTextChangedListener(new android.text.TextWatcher() {
                public void beforeTextChanged(CharSequence s,int st,int c,int a) {}
                public void onTextChanged(CharSequence s,int st,int before,int count) { updateLabels(); refresh(); }
                public void afterTextChanged(android.text.Editable e) {}
            });
            col.addView(nameInputs[i], new LinearLayout.LayoutParams(-1, dp(48)));
            namesRow.addView(col, new LinearLayout.LayoutParams(0, -2, 1));
        }
        root.addView(namesRow);

        section(root, "2. Add an expense");
        descInput = edit("Description (e.g. dinner, petrol)", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        root.addView(descInput, new LinearLayout.LayoutParams(-1, dp(50)));
        amountInput = edit("Total amount (₹)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(amountInput, new LinearLayout.LayoutParams(-1, dp(50)));
        root.addView(text("Who paid?", 13, false));
        payerSpinner = new Spinner(this);
        root.addView(payerSpinner, new LinearLayout.LayoutParams(-1, dp(48)));
        root.addView(text("Split method", 13, false));
        splitModeSpinner = new Spinner(this);
        ArrayAdapter<String> modes = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Split equally among selected friends", "Custom amount per friend"});
        splitModeSpinner.setAdapter(modes);
        root.addView(splitModeSpinner, new LinearLayout.LayoutParams(-1, dp(48)));
        root.addView(text("Who shares this bill?", 13, false));
        for (int i=0;i<3;i++) {
            final int idx=i;
            participantChecks[i] = new CheckBox(this);
            participantChecks[i].setText(defaultNames[i]);
            participantChecks[i].setChecked(true);
            root.addView(participantChecks[i]);
        }
        root.addView(text("Custom shares (₹) — fill only when using custom split", 12, false));
        LinearLayout customRow = horizontal();
        for (int i=0;i<3;i++) {
            customInputs[i] = edit(new String[]{"Friend 1 ₹","Friend 2 ₹","Friend 3 ₹"}[i],
                    InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            customRow.addView(customInputs[i], new LinearLayout.LayoutParams(0, dp(50), 1));
        }
        root.addView(customRow);
        Button add = button("+ Add expense");
        root.addView(add, new LinearLayout.LayoutParams(-1, dp(52)));
        add.setOnClickListener(v -> addBill());

        section(root, "3. Summary");
        totalView = text("Total expenses: ₹0.00", 18, true);
        averageView = text("Average per friend: ₹0.00", 14, false);
        root.addView(totalView);
        root.addView(averageView);
        summaryBox = vertical();
        root.addView(summaryBox);

        section(root, "4. Who pays whom?");
        settlementView = text("Add a bill to calculate payments.", 15, false);
        root.addView(settlementView);
        Button share = button("Share settlement on WhatsApp");
        root.addView(share, new LinearLayout.LayoutParams(-1, dp(50)));
        share.setOnClickListener(v -> shareSettlement());

        section(root, "5. Bill history");
        historyBox = vertical();
        root.addView(historyBox);
        Button reset = button("Reset all bills");
        root.addView(reset, new LinearLayout.LayoutParams(-1, dp(48)));
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete all bills?")
                .setMessage("This permanently clears the saved bill history on this device.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete all", (d,w) -> { bills.clear(); nextId=1; saveBills(); refresh(); })
                .show());
        updateLabels();
    }

    private void updateLabels() {
        String[] n=names();
        if (participantChecks != null) for(int i=0;i<3;i++) if(participantChecks[i]!=null) participantChecks[i].setText(n[i]);
        if (payerSpinner != null) {
            int old=payerSpinner.getSelectedItemPosition();
            ArrayAdapter<String> a=new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,n);
            payerSpinner.setAdapter(a);
            payerSpinner.setSelection(Math.min(old,2));
        }
    }

    private void addBill() {
        String raw=amountInput.getText().toString().trim();
        double amount;
        try { amount=Double.parseDouble(raw); } catch(Exception e) { toast("Enter a valid amount."); return; }
        if (!Double.isFinite(amount) || amount<=0 || amount>100000000) { toast("Amount must be greater than ₹0."); return; }
        amount=Math.round(amount*100.0)/100.0;
        List<Integer> people=new ArrayList<>();
        for(int i=0;i<3;i++) if(participantChecks[i].isChecked()) people.add(i);
        if(people.isEmpty()) { toast("Select at least one person who shares this bill."); return; }
        boolean custom=splitModeSpinner.getSelectedItemPosition()==1;
        double[] shares=new double[3];
        if(custom) {
            double sum=0;
            for(int i=0;i<3;i++) {
                String s=customInputs[i].getText().toString().trim();
                double val=0;
                if(!s.isEmpty()) try { val=Double.parseDouble(s); } catch(Exception ex) { toast("Check the custom share amounts."); return; }
                if(val<0 || !Double.isFinite(val)) { toast("Custom shares cannot be negative."); return; }
                if(!people.contains(i) && val>0.0001) { toast(names()[i]+" is not selected to share this bill."); return; }
                shares[i]=Math.round(val*100.0)/100.0;
                sum+=shares[i];
            }
            if(Math.abs(sum-amount)>0.009) { toast("Custom shares must add up to "+money(amount)+"."); return; }
            for(int i=0;i<3;i++) if(!people.contains(i) && shares[i]>0) { toast("Unselected friends must have ₹0 share."); return; }
        } else {
            int cents=(int)Math.round(amount*100);
            int base=cents/people.size(), remainder=cents%people.size();
            for(int k=0;k<people.size();k++) shares[people.get(k)]=(base+(k<remainder?1:0))/100.0;
        }
        JSONObject b=new JSONObject();
        try {
            b.put("id",nextId++); b.put("description",descInput.getText().toString().trim().isEmpty()?"Expense":descInput.getText().toString().trim());
            b.put("amount",amount); b.put("payer",payerSpinner.getSelectedItemPosition());
            JSONArray sa=new JSONArray(); for(double s:shares) sa.put(Math.round(s*100.0)/100.0); b.put("shares",sa);
            bills.add(b); saveBills(); amountInput.setText(""); descInput.setText("");
            for(EditText e:customInputs)e.setText("");
            toast("Expense saved on this device."); refresh();
        } catch(Exception ex) { toast("Could not save this bill."); }
    }

    private double[] paidTotals() {
        double[] paid=new double[3];
        for(JSONObject b:bills) paid[b.optInt("payer",0)]+=b.optDouble("amount",0);
        return paid;
    }
    private double[] shareTotals() {
        double[] shares=new double[3];
        for(JSONObject b:bills) {
            JSONArray a=b.optJSONArray("shares");
            if(a!=null) for(int i=0;i<3;i++) shares[i]+=a.optDouble(i,0);
        }
        return shares;
    }
    private double[] balances() {
        double[] p=paidTotals(),s=shareTotals(),b=new double[3];
        for(int i=0;i<3;i++) b[i]=Math.round((p[i]-s[i])*100.0)/100.0;
        return b;
    }

    private void refresh() {
        if(summaryBox==null)return;
        String[] n=names();
        double total=0; for(JSONObject b:bills) total+=b.optDouble("amount",0);
        totalView.setText("Total expenses: "+money(total));
        averageView.setText("Average per friend: "+money(total/3.0));
        summaryBox.removeAllViews();
        double[] paid=paidTotals(), shares=shareTotals(), bal=balances();
        for(int i=0;i<3;i++) {
            summaryBox.addView(text(n[i]+"  ·  Paid "+money(paid[i])+"  ·  Share "+money(shares[i]),14,false));
            String status=bal[i]>0.004?"Gets back "+money(bal[i]):bal[i]<-0.004?"Owes "+money(-bal[i]):"Settled";
            TextView t=text("   "+status,13,true); t.setTextColor(bal[i]<-0.004?0xFFB3261E:0xFF137333); summaryBox.addView(t);
        }
        StringBuilder settlement=new StringBuilder();
        int[] cents=new int[3]; for(int i=0;i<3;i++) cents[i]=(int)Math.round(bal[i]*100);
        List<Integer> debtors=new ArrayList<>(), creditors=new ArrayList<>();
        for(int i=0;i<3;i++){if(cents[i]<0)debtors.add(i);if(cents[i]>0)creditors.add(i);}
        int di=0,ci=0;
        while(di<debtors.size()&&ci<creditors.size()){
            int d=debtors.get(di),c=creditors.get(ci),amt=Math.min(-cents[d],cents[c]);
            if(amt>0) {
                if(settlement.length()>0)settlement.append("\n\n");
                settlement.append(n[d]).append(" pays ").append(n[c]).append(": ").append(money(amt/100.0));
                cents[d]+=amt;cents[c]-=amt;
            }
            if(cents[d]==0)di++; if(cents[c]==0)ci++;
        }
        if(settlement.length()==0) settlement.append(bills.isEmpty()?"Add a bill to calculate payments.":"Everyone is settled. No payments needed.");
        settlementView.setText(settlement.toString());

        historyBox.removeAllViews();
        if(bills.isEmpty()) historyBox.addView(text("No expenses added yet.",14,false));
        for(int j=bills.size()-1;j>=0;j--){
            JSONObject b=bills.get(j);
            LinearLayout row=horizontal(); row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout details=vertical();
            details.addView(text(b.optString("description","Expense")+" · "+money(b.optDouble("amount",0)),15,true));
            int payer=b.optInt("payer",0);
            details.addView(text("Paid by "+n[Math.max(0,Math.min(2,payer))],12,false));
            row.addView(details,new LinearLayout.LayoutParams(0,-2,1));
            Button del=button("Delete");
            del.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Delete this bill?")
                    .setMessage(b.optString("description","Expense")+" will be removed from the saved history.")
                    .setNegativeButton("Cancel",null).setPositiveButton("Delete",(dialog,which)->{bills.remove(b);saveBills();refresh();}).show());
            row.addView(del);
            historyBox.addView(row);
            View line=new View(this);line.setBackgroundColor(0xFFE5E7EB);
            historyBox.addView(line,new LinearLayout.LayoutParams(-1,dp(1)));
        }
    }

    private void shareSettlement() {
        String[] n=names(); double total=0;for(JSONObject b:bills)total+=b.optDouble("amount",0);
        StringBuilder msg=new StringBuilder("💸 Split Bill — 3 Friends\n\n");
        msg.append("Total expenses: ").append(money(total)).append("\n\n");
        double[] p=paidTotals(),s=shareTotals(),bal=balances();
        for(int i=0;i<3;i++)msg.append(n[i]).append(": paid ").append(money(p[i])).append(", share ").append(money(s[i])).append("\n");
        msg.append("\nSettlement:\n");
        int[] cents=new int[3];for(int i=0;i<3;i++)cents[i]=(int)Math.round(bal[i]*100);
        boolean any=false;
        for(int guard=0;guard<3;guard++){
            int d=-1,c=-1;
            for(int i=0;i<3;i++)if(cents[i]<0&&(d<0||cents[i]<cents[d]))d=i;
            for(int i=0;i<3;i++)if(cents[i]>0&&(c<0||cents[i]>cents[c]))c=i;
            if(d<0||c<0)break;
            int amt=Math.min(-cents[d],cents[c]);
            if(amt>0){msg.append(n[d]).append(" pays ").append(n[c]).append(": ").append(money(amt/100.0)).append("\n");any=true;cents[d]+=amt;cents[c]-=amt;}
        }
        if(!any)msg.append(bills.isEmpty()?"No bills added yet.":"Everyone is settled.");
        msg.append("\n\nGenerated with Split Bill 3");
        Intent intent=new Intent(Intent.ACTION_SEND);intent.setType("text/plain");intent.putExtra(Intent.EXTRA_TEXT,msg.toString());
        try { startActivity(Intent.createChooser(intent,"Share settlement")); }
        catch(Exception e){toast("No compatible sharing app found.");}
    }

    private void saveBills() {
        JSONArray array=new JSONArray();for(JSONObject b:bills)array.put(b);
        prefs.edit().putString("bills",array.toString()).putInt("nextId",nextId).apply();
    }
    private void loadBills() {
        nextId=prefs.getInt("nextId",1);
        try {JSONArray a=new JSONArray(prefs.getString("bills","[]"));for(int i=0;i<a.length();i++)bills.add(a.getJSONObject(i));}
        catch(Exception ignored){bills.clear();}
    }
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
}
