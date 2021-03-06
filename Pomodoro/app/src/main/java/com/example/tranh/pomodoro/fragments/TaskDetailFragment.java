package com.example.tranh.pomodoro.fragments;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.example.tranh.pomodoro.R;
import com.example.tranh.pomodoro.activities.TaskActivity;
import com.example.tranh.pomodoro.adapters.ColorApdapter;
import com.example.tranh.pomodoro.database.DbContext;
import com.example.tranh.pomodoro.database.models.Task;
import com.example.tranh.pomodoro.decorations.TaskColorDecoration;
import com.example.tranh.pomodoro.evenbus_event.DataChange;
import com.example.tranh.pomodoro.evenbus_event.TaskAction;
import com.example.tranh.pomodoro.networks.NetContext;
import com.example.tranh.pomodoro.networks.services.TaskActionService;
import com.example.tranh.pomodoro.utils.TaskActionEnum;
import com.example.tranh.pomodoro.utils.Util;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * A simple {@link Fragment} subclass.
 */
public class TaskDetailFragment extends Fragment {
    private final String TAG = TaskDetailFragment.class.toString();
    private ColorApdapter colorApdapter;
    @BindView(R.id.rv_color)
    RecyclerView rv_color;
    @BindView(R.id.ed_pertask)
    EditText et_perTask;
    @BindView(R.id.ed_nametask)
    EditText et_nameTask;
    private String tittle;
    private Task task;
    int count;
    ProgressDialog dialog;

    String done;
    String failed;


    private final DbContext dbContext = new DbContext(getContext());

    public TaskActionEnum getTaskAction() {
        return taskAction;
    }

    public void setTaskAction(TaskActionEnum taskAction) {
        this.taskAction = taskAction;
    }

    private TaskActionEnum taskAction;


    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public String getTittle() {
        return tittle;
    }

    public void setTittle(String tittle) {
        this.tittle = tittle;
    }

    public TaskDetailFragment() {
        setHasOptionsMenu(true);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        dialog = ProgressDialog.show(getContext(), getString(R.string.loadding),
                getString(R.string.please_wait), true);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void setupUI(View view) {
        ButterKnife.bind(this, view);

        dialog.dismiss();
        colorApdapter = new ColorApdapter();
        rv_color.setAdapter(colorApdapter);
        rv_color.setLayoutManager(new GridLayoutManager(this.getContext(), 4));
        rv_color.addItemDecoration(new TaskColorDecoration());
        if (task != null) {
            et_nameTask.setText(task.getName());
            et_perTask.setText(String.format("%.1f", task.getPaymentPerHour()));
            colorApdapter.setSelectColor(task.getColor());
        }
        if (getActivity() instanceof TaskActivity) {
            ((TaskActivity) getActivity()).getSupportActionBar().setTitle(tittle);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_detail, container, false);
        setupUI(view);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_edit_task, menu);
        menu.removeItem(R.id.action_settings);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.mni_ok) {
            Util.hideSoftKeyboard(getActivity());
            dialog.show();
            final String taskname = et_nameTask.getText().toString();
            final String payment = et_perTask.getText().toString();
            float paymentPerHour = getPayment(payment);
            final String color = colorApdapter.getSelectColor();
            final Task newTask = new Task(UUID.randomUUID().toString(), taskname, color, paymentPerHour, false);
            if (!taskname.isEmpty()) {
                if (taskAction == TaskActionEnum.ADD_NEW) {
                    TaskActionService addNewTask = NetContext.instance.create(TaskActionService.class);
                    addNewTask.addNewTask(newTask).enqueue(new Callback<Task>() {
                        @Override
                        public void onResponse(Call<Task> call, Response<Task> response) {
                            Log.e(TAG, String.format("onResponse: %s", response.body().toString()));
                            EventBus.getDefault().post(new DataChange("Hoàn thành"));
                            dialog.dismiss();
                        }

                        @Override
                        public void onFailure(Call<Task> call, Throwable t) {
                            Log.e(TAG, String.format("onFailure: %s", t.toString()));
                            EventBus.getDefault().post(new DataChange("Không thành công"));
                            dialog.dismiss();
                        }
                    });
                }
                if (taskAction == TaskActionEnum.EDIT) {
                    TaskActionService editTask = NetContext.instance.create(TaskActionService.class);
                    editTask.editTask(task.getId(), newTask).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response) {
                            EventBus.getDefault().post(new DataChange("Hoàn thành"));
                            dialog.dismiss();
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            Log.e(TAG, String.format("onFailure: %d %s", count, t.toString()));
                            EventBus.getDefault().post(new DataChange("Không thành công"));
                            dialog.dismiss();
                        }
                    });
                }
                getActivity().onBackPressed();// gọi onResume???
            } else {
                et_nameTask.setError(getString(R.string.name_not_empty));
            }
        }
        return false;
    }


    public float getPayment(String payment) {
        float paymentPerHour;
        try {
            if (payment.isEmpty()) {
                paymentPerHour = 0;
            } else {
                paymentPerHour = Float.parseFloat(payment);
            }
        } catch (NumberFormatException e) {
            String[] str = payment.split(",");
            // nó đổi . thành , nên làm ntn
            paymentPerHour = Float.parseFloat(str[0] + "." + str[1]);
        }
        return paymentPerHour;

    }
}
