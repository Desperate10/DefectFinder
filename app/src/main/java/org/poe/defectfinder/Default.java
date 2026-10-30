package org.poe.defectfinder;

public class Default {
    //модификторы csv
    public static String ADD_MODIFICATOR = "add";
    public static String EDIT_MODIFICATOR = "edit";
    public static String CONFIRM_MODIFICATOR = "confirm";

    //повторна відправка вже вигруженого листка (остання колонка csv)
    public static final String RESEND_MARKER = "E";
    //скільки часу після першої відправки листок можна редагувати
    public static final long EDIT_WINDOW_MS = 24 * 60 * 60 * 1000L;

    //модификатор обходного листа
//    public static String FIND_DEFECT = "find";
//    public static String REMOVE_DEFECT = "clear";

    public static boolean isHighVoltage(int rawResource) {
        return rawResource == R.raw.line35 || rawResource == R.raw.ps;
    }
}
