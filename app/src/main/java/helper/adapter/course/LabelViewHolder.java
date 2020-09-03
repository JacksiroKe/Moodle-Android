package helper.adapter.course;

import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jacksiroke.moodle.R;
import helper.Util;
import set.Module;

public class LabelViewHolder extends RecyclerView.ViewHolder {

    private TextView textView;

    public LabelViewHolder(@NonNull View itemView) {
        super(itemView);

        textView = itemView.findViewById(R.id.text);

        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void bind(Module m) {

        if (m.getExpandableTextDisplay().text == null)
            m.getExpandableTextDisplay().text = Util.fromHtml(m.getDescription());

        Util.setTextExpandable(textView, m.getExpandableTextDisplay());
    }
}
