package com.simplemobiletools.filemanager.pro.dialogs

import android.view.View
import androidx.appcompat.app.AlertDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.filemanager.pro.R
import com.simplemobiletools.filemanager.pro.activities.SimpleActivity
import com.simplemobiletools.filemanager.pro.helpers.RootHelpers
import com.simplemobiletools.filemanager.pro.helpers.Config
import kotlinx.android.synthetic.main.dialog_create_new.view.*
import kotlinx.android.synthetic.main.dialog_save_as.view.*
import java.io.File
import java.io.IOException

import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.extensions.*

class CreateNewItemDialog(val activity: BaseSimpleActivity, var path: String, val callback: (success: Boolean) -> Unit) {
    private val view = activity.layoutInflater.inflate(R.layout.dialog_create_new, null)

    init {
        //if (path.isEmpty()) {
        //    path = "${activity.internalStoragePath}/${activity.getCurrentFormattedDateTime()}.txt"
        //}

        var realPath = path.toString()
        val view = activity.layoutInflater.inflate(R.layout.dialog_create_new, null).apply {
            create_as_path.text = activity.humanizePath(realPath)

            create_as_path.setOnClickListener {
                FilePickerDialog(activity, realPath, false, false, true, true, showFavoritesButton = true) {
                    create_as_path.text = activity.humanizePath(it)
                    realPath = it
                }
            }
        }

        AlertDialog.Builder(activity)
            .setPositiveButton(R.string.ok, null)
            .setNegativeButton(R.string.cancel, null)
            //.setPositiveButton("Clean",null)

            .create().apply {
                activity.setupDialogStuff(view, this, R.string.create_new) {
                    //keyboard
                    showKeyboard(view.item_name)

                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(View.OnClickListener {
                        val name = view.item_name.value
                        if (name.isEmpty()) {
                            //activity.toast(R.string.empty_name)
                            //openPath(config.homeFolder)
                        } else if (name.isAValidFilename()) {
                            val newPath = "$realPath/$name"
                            if (activity.getDoesFilePathExist(newPath)) {
                                activity.toast(R.string.name_taken)
                                return@OnClickListener
                            }

                            if (view.dialog_radio_group.checkedRadioButtonId == R.id.dialog_radio_directory) {
                                createDirectory(newPath, this) {
                                    callback(it)
                                }
                            }
                            //modify:add dialog_radio_zip
                            else if(view.dialog_radio_group.checkedRadioButtonId == R.id.dialog_radio_file){
                                createFile(newPath, this) {
                                    //config.addFavorite(fragment.currentPath)
                                    //callback(it)
                                }
                            }
                            else{
                                val newPath = "$newPath/$name.zip"
                                createFile(newPath, this) {
                                    callback(it)
                                }

                            }
                        } else {
                            activity.toast(R.string.invalid_name)
                        }
                    })
                }
            }
    }

    private fun createDirectory(path: String, alertDialog: AlertDialog, callback: (Boolean) -> Unit) {
        when {
            activity.needsStupidWritePermissions(path) -> activity.handleSAFDialog(path) {
                if (!it) {
                    return@handleSAFDialog
                }

                val documentFile = activity.getDocumentFile(path.getParentPath())
                if (documentFile == null) {
                    val error = String.format(activity.getString(R.string.could_not_create_folder), path)
                    activity.showErrorToast(error)
                    callback(false)
                    return@handleSAFDialog
                }
                documentFile.createDirectory(path.getFilenameFromPath())
                success(alertDialog)
            }
            path.startsWith(activity.internalStoragePath, true) -> {
                if (File(path).mkdirs()) {
                    success(alertDialog)
                }
            }
            else -> {
                RootHelpers(activity).createFileFolder(path, false) {
                    if (it) {
                        success(alertDialog)
                    } else {
                        callback(false)
                    }
                }
            }
        }
    }

    private fun createFile(path: String, alertDialog: AlertDialog, callback: (Boolean) -> Unit) {
        try {
            when {
                activity.needsStupidWritePermissions(path) -> {
                    activity.handleSAFDialog(path) {
                        if (!it) {
                            return@handleSAFDialog
                        }

                        val documentFile = activity.getDocumentFile(path.getParentPath())
                        if (documentFile == null) {
                            val error = String.format(activity.getString(R.string.could_not_create_file), path)
                            activity.showErrorToast(error)
                            callback(false)
                            return@handleSAFDialog
                        }
                        documentFile.createFile(path.getMimeType(), path.getFilenameFromPath())
                        success(alertDialog)
                    }
                }
                path.startsWith(activity.internalStoragePath, true) -> {
                    if (File(path).createNewFile()) {
                        success(alertDialog)
                    }
                }
                else -> {
                    RootHelpers(activity).createFileFolder(path, true) {
                        if (it) {
                            success(alertDialog)
                        } else {
                            callback(false)
                        }
                    }
                }
            }
        } catch (exception: IOException) {
            activity.showErrorToast(exception)
            callback(false)
        }
    }

    private fun success(alertDialog: AlertDialog) {
        alertDialog.dismiss()
        callback(true)
    }
}
