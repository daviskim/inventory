/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.inventory;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.example.android.inventory.data.ProductContract;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.DecimalFormat;

import static com.example.android.inventory.R.id.price;

/**
 * Allows user to enter a new product or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    /** Identifier for the product data loader */
    private static final int PRODUCT_LOADER = 0;

    private static final int SELECT_FILE = 1;

    private static final int RESULT_LOAD_IMAGE = 1;

    /** Content URI for the existing product (null if it's a new product) */
    private Uri mCurrentProductUri;

    /** EditText field to enter the product's name */
    private EditText mNameEditText;

    /** EditText field to enter the product's price */
    private EditText mPriceEditText;

    /** EditText field to enter the product's quantity */
    private EditText mQuantityEditText;

    /** EditText field to enter the product's quantity sold */
    private EditText mSoldEditText;

    /** ImageView for holding product image */
    private ImageView mImageView;

    /** Boolean flag that keeps track of whether the product has been edited (true)
     *  or not (false)
     */
    private boolean mProductHasChanged = false;

    /**
     * OnTouchListener that listens for any user touches on a view, implying that they
     * are modifying the view, and we change the mPetHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            mProductHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're entering a new product or editing an existing one.
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        // If the intent DOES NOT contain a product content URI, then we know that we are
        // entering a new product.
        if (mCurrentProductUri == null) {
            // This is a new product, so change the app bar to say "Add a Product"
            setTitle(getString(R.string.editor_activity_title_new_product));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing product, so change app bar to say "Edit Product"
            setTitle(getString(R.string.editor_activity_title_edit_product));

            // Initialize a loader to read the product data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(PRODUCT_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        mNameEditText = (EditText) findViewById(R.id.edit_product_name);
        mPriceEditText = (EditText) findViewById(R.id.edit_price);
        mQuantityEditText = (EditText) findViewById(R.id.edit_quantity);
        mSoldEditText = (EditText) findViewById(R.id.edit_sold);
        mImageView = (ImageView) findViewById(R.id.product_image);

        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        mNameEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mQuantityEditText.setOnTouchListener(mTouchListener);
        mSoldEditText.setOnTouchListener(mTouchListener);
    }

    // Get user input from editor and save product into datatbase.
    private void saveProduct() {
        String nameString = mNameEditText.getText().toString().trim();
        String priceString = mPriceEditText.getText().toString().trim();
        String quantityString = mQuantityEditText.getText().toString().trim();
        String soldString = mSoldEditText.getText().toString().trim();
        // This converts image into a byteArray so that it can be saved in the database.
        BitmapDrawable bitmapDrawable = (BitmapDrawable) mImageView.getDrawable();
        // Check if an image was selected, if not, then return
        if (bitmapDrawable == null) {
            Toast.makeText(this, getString(R.string.editor_image_not_selected),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap imageBitmap = bitmapDrawable.getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        byte[] image = stream.toByteArray();

        // Check if this is supposed to be a new product
        // and check if the name or price fields in the editor are blank
        if (mCurrentProductUri == null &&
                TextUtils.isEmpty(nameString) || TextUtils.isEmpty(priceString)) {
            // If the name or price is blank, then show toast and do not save product info.
            Toast.makeText(this, getString(R.string.editor_save_no_information),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Extract $ from price, if it was added
        priceString = priceString.replace("$", "");

        ContentValues values = new ContentValues();
        values.put(ProductContract.ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(ProductContract.ProductEntry.COLUMN_PRODUCT_PRICE, priceString);
        values.put(ProductContract.ProductEntry.COLUMN_PRODUCT_QUANTITY, quantityString);
        values.put(ProductContract.ProductEntry.COLUMN_PRODUCT_SOLD, soldString);
        values.put(ProductContract.ProductEntry.COLUMN_PRODUCT_IMAGE, image);
        // If the quantity is not provided by the user, don't try to parse the string into an
        // integer value. Use 0 by default.
        int quantity = 0;
        if (!TextUtils.isEmpty(quantityString)) {
            quantity = Integer.parseInt(quantityString);
        }
        values.put(ProductContract.ProductEntry.COLUMN_PRODUCT_QUANTITY, quantity);

        // Determine if this is a new or existing product by checking if mCurrentProductUri
        // is null or not
        if (mCurrentProductUri == null) {
            // This is a NEW product, so insert a new product into the provider,
            // returning the content URI for the new product.
            Uri newUri = getContentResolver().insert(ProductContract.ProductEntry.CONTENT_URI,
                    values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING product, so update the product with content URI:
            // mCurrentProductUri and pass in the new ContentValues. Pass in null for the
            // selection and selection args because mCurrentProductUri will already identify
            // the correct row in the database that we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_update_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_update_product_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Perform the deletion of the product in the database when "Delete" button is pressed.
     */
    public void deleteButton(View view) {
        // Only perform the delete if this is an existing product.
        if (mCurrentProductUri != null) {
            // Code for pop up dialog box confirming deletion of product.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.confirm_deletion);
            builder.setCancelable(true);

            builder.setPositiveButton(
                    R.string.yes,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Call the ContentResolver to delete the product at the given content
                            // URI. Pass in null for the selection and selection args because the
                            // mCurrentProductUri content URI already identifies the product that
                            // we want.
                            int rowsDeleted = getContentResolver().delete(mCurrentProductUri,
                                    null, null);

                            // Show a toast message depending on whether or not the
                            // delete was successful.
                            if (rowsDeleted == 0) {
                                // If no rows were deleted, then there was an error with
                                // the delete.
                                Toast.makeText(EditorActivity.this,
                                        getString(R.string.editor_delete_product_failed),
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                // Otherwise, the delete was successful and we can
                                // display a toast.
                                Toast.makeText(EditorActivity.this,
                                        getString(R.string.editor_delete_product_successful),
                                        Toast.LENGTH_SHORT).show();
                            }
                            // Close the activity
                            finish();
                        }
                    }
            );
            builder.setNegativeButton(
                    R.string.no,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Don't do anything and stay on the product detail page
                        }
                    }
            );
            AlertDialog alert = builder.create();
            alert.show();
        } else if (mCurrentProductUri == null) {
            // If all fields are blank then display "nothing to delete".
            Toast.makeText(this, getString(R.string.editor_nothing_to_delete),
                    Toast.LENGTH_SHORT).show();
            // Close the activity
            finish();
        }
    }

    /**
     * Decrements current quantity and increments sold quantity when "Sold" button is pressed.
     */
    public void soldButton(View view) {
        // When sold button is pressed, this code decrements the quantity field.
        String quantityString = mQuantityEditText.getText().toString().trim();
        int quantityInt = Integer.parseInt(quantityString);
        // Make sure inventory quantity does not go negative
        if (quantityInt > 0) {
            quantityInt--;
            mQuantityEditText.setText(Integer.toString(quantityInt));

            // When sold button is pressed, this code increments the quantity sold field.
            String quantitySoldString = mSoldEditText.getText().toString().trim();
            int quantitySoldInt = Integer.parseInt(quantitySoldString);
            quantitySoldInt++;
            mSoldEditText.setText(Integer.toString(quantitySoldInt));
        } else {
            // Quantity is 0 then show Toast saying no more inventory available for sale.
            Toast.makeText(this, getString(R.string.editor_no_inventory),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receive qty button increments current quantity by 1.
     */
    public void receiveButton(View view) {
        // When receive qty button is pressed, this increments the quantity field by 1.
        String quantityString = mQuantityEditText.getText().toString().trim();
        int quantityInt = Integer.parseInt(quantityString);
        quantityInt++;
        mQuantityEditText.setText(Integer.toString(quantityInt));
    }

    /**
     * Remove qty button decrements current quantity by 1.
     */
    public void removeButton(View view) {
        // When remove qty button is pressed, this code decrements the quantity field.
        String quantityString = mQuantityEditText.getText().toString().trim();
        int quantityInt = Integer.parseInt(quantityString);
        // Make sure inventory quantity does not go negative
        if (quantityInt > 0) {
            quantityInt--;
            mQuantityEditText.setText(Integer.toString(quantityInt));
        } else {
            // Quantity is 0 then show Toast saying no more inventory available for sale.
            Toast.makeText(this, getString(R.string.editor_no_inventory),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Order button places an order for the product via email.
     */
    public void orderButton(View view) {
        // When order button is pressed, this code opens a blank email and places an order.
        String nameString = mNameEditText.getText().toString().trim();

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Order for more " + nameString);
        intent.putExtra(Intent.EXTRA_TEXT, "Please place an order for " + nameString);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save product in database
                saveProduct();
                // Exit activity
                finish();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the product hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!mProductHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (!mProductHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Since the editor shows all product attributes, define a projection that contains
        // all columns from the product table
        String[] projection = {
                ProductContract.ProductEntry._ID,
                ProductContract.ProductEntry.COLUMN_PRODUCT_NAME,
                ProductContract.ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductContract.ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductContract.ProductEntry.COLUMN_PRODUCT_SOLD,
                ProductContract.ProductEntry.COLUMN_PRODUCT_IMAGE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentProductUri,         // Query the content URI for the current product
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of product attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex
                    (ProductContract.ProductEntry.COLUMN_PRODUCT_NAME);
            int priceColumnIndex = cursor.getColumnIndex
                    (ProductContract.ProductEntry.COLUMN_PRODUCT_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex
                    (ProductContract.ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int soldColumnIndex = cursor.getColumnIndex
                    (ProductContract.ProductEntry.COLUMN_PRODUCT_SOLD);
            int imageColumnIndex = cursor.getColumnIndex
                    (ProductContract.ProductEntry.COLUMN_PRODUCT_IMAGE);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            double price = cursor.getDouble(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            int sold = cursor.getInt(soldColumnIndex);
            byte[] image = cursor.getBlob(imageColumnIndex);

            // Force two decimal places to show in price
            DecimalFormat decFor = new DecimalFormat("#.00");

            // Update the views on the screen with the values from the database
            mNameEditText.setText(name);
            mPriceEditText.setText("$" + String.valueOf(decFor.format(price)));
            mQuantityEditText.setText(Integer.toString(quantity));
            mSoldEditText.setText(Integer.toString(sold));
            mImageView.setImageBitmap(BitmapFactory.decodeByteArray(image, 0, image.length));
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mNameEditText.setText("");
        mPriceEditText.setText("");
        mQuantityEditText.setText("");
        mSoldEditText.setText("");
        mImageView.setImageBitmap(null);
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "keep editing" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // This will get an image from one of the image apps like "Gallery".
    public void loadImage(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, SELECT_FILE);
    }

    // Once image is selected, then need to resize the image to fit the ImageView.
    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        ImageView imageView = (ImageView) findViewById(R.id.product_image);
        Bitmap bitmapImage;
        Bitmap resizedImage;

        try {
            // When image is picked
            if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) {

                InputStream stream = getContentResolver().openInputStream(data.getData());
                bitmapImage = BitmapFactory.decodeStream(stream);
                resizedImage = Bitmap.createScaledBitmap(bitmapImage, 75, 75, true);
                stream.close();

                // Set the image in ImageView after decoding the string
                imageView.setImageBitmap(resizedImage);
            } else {
                Toast.makeText(this, R.string.editor_image_not_selected, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.editor_image_error, Toast.LENGTH_SHORT).show();
        }
    }
}