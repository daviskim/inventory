package com.example.android.inventory;

/**
 * Created by DK on 10/7/2016.
 */

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.example.android.inventory.data.ProductContract;

import java.text.DecimalFormat;

/**
 * {@link ProductCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of product data as its data source. This adapter knows
 * how to create list items for each row of product data in the {@link Cursor}.
 */
public class ProductCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link ProductCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor The cursor from which to get the data. The cursor is already
     *               moved to the correct position.
     * @param parent The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    /**
     * This method binds the product data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current product can be set on the name
     * TextView in the list item layout.
     *
     * @param view Existing view, returned earlier by newView() method
     * @param CONTEXT app context
     * @param cursor The cursor from which to get the data. The cursor is already moved to the
     *               correct row.
     */
    @Override
    public void bindView(View view, final Context CONTEXT, Cursor cursor) {
        // CONTEXT is final so it can be used in onClick method below.
        // Find individual views that we want to modify in the list item layout
        TextView nameTextView = (TextView) view.findViewById(R.id.name);
        TextView priceTextView = (TextView) view.findViewById(R.id.price);
        final TextView quantityTextView = (TextView) view.findViewById(R.id.quantity_current);
        final TextView soldTextView = (TextView) view.findViewById(R.id.quantity_sold);

        // Find the columns of product attributes that we're interested in
        int idColumnIndex =
                cursor.getColumnIndex(ProductContract.ProductEntry._ID);
        // idColumnIndex is to assign a number to each button on each list item in
        // Catalog Activity
        int nameColumnIndex =
                cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRODUCT_NAME);
        int priceColumnIndex =
                cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRODUCT_PRICE);
        int quantityColumnIndex =
                cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRODUCT_QUANTITY);
        int soldColumnIndex =
                cursor.getColumnIndex(ProductContract.ProductEntry.COLUMN_PRODUCT_SOLD);

        // Read the product attributes from the Cursor for the current product
        int id = cursor.getInt(idColumnIndex);
        String productName = cursor.getString(nameColumnIndex);
        double productPrice = cursor.getDouble(priceColumnIndex);
        int productQuantity = cursor.getInt(quantityColumnIndex);
        int soldQuantity = cursor.getInt(soldColumnIndex);

        // Force two decimal places to show in price
        DecimalFormat decFor = new DecimalFormat("#.00");

        // Add $ symbol to price
        String stringPrice = "$" + String.valueOf(decFor.format(productPrice));

        // Update the TextViews with the attributes for the current product
        nameTextView.setText(productName);
        priceTextView.setText(stringPrice);
        quantityTextView.setText(String.valueOf(productQuantity));
        soldTextView.setText(String.valueOf(soldQuantity));

        // Need productQuantity & soldQuantity number to be available in onClick method.
        final int CURR_QTY = productQuantity;
        final int SOLD_QTY = soldQuantity;

        final Button listViewSoldButton = (Button) view.findViewById(R.id.listViewSold);
        listViewSoldButton.setTag(id); // Assign id to each button on list view
        listViewSoldButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int mId = (Integer) view.getTag(); // get id of the button clicked on

                // Make sure inventory quantity does not go negative
                if (CURR_QTY > 0) {
                    ContentValues listItemsValues = new ContentValues();
                    listItemsValues.put(ProductContract.ProductEntry.
                            COLUMN_PRODUCT_QUANTITY, CURR_QTY - 1); // subtract 1 from inventory
                    listItemsValues.put(ProductContract.ProductEntry.
                            COLUMN_PRODUCT_SOLD, SOLD_QTY + 1); // add 1 to sold count

                    Uri currentProductUri = ContentUris.withAppendedId
                            (ProductContract.ProductEntry.CONTENT_URI, mId);
                    CONTEXT.getContentResolver().update
                            (currentProductUri, listItemsValues, null, null);

                    quantityTextView.setText(String.valueOf(CURR_QTY - 1)); // display current qty
                    soldTextView.setText(String.valueOf(SOLD_QTY + 1)); // display sold qty
                } else {
                    // If quantity is 0 then show Toast saying no more inventory
                    // available for sale.
                    Toast.makeText(CONTEXT, CONTEXT.getString(R.string.editor_no_inventory),
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}