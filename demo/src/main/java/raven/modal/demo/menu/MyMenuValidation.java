package raven.modal.demo.menu;

import raven.modal.Drawer;
import raven.modal.demo.model.ModelUser;
import raven.modal.demo.system.Form;
import raven.modal.drawer.menu.MenuValidation;

/**
 * Custom menu validation logic based on the current user and menu index.
 * Extends {@link MenuValidation} and provides static methods to validate
 * menu access for different user roles. Admin users are given full access,
 * whereas other roles can be restricted (current implementation allows all).
 */
public class MyMenuValidation extends MenuValidation {

    /**
     * The current user whose permissions are used for menu validation.
     */
    public static ModelUser user;

    /**
     * Sets the current user whose role will determine menu validation.
     *
     * @param user the {@link ModelUser} object representing the signed-in user
     */
    public static void setUser(ModelUser user) {
        MyMenuValidation.user = user;
    }

    /**
     * Validates if the provided menu index is accessible for the current user.
     * Delegates to {@link #validation(int[])}.
     *
     * @param index the array of menu indices representing the navigation path
     * @return true if the menu is accessible, false otherwise
     */
    @Override
    public boolean menuValidation(int[] index) {
        return validation(index);
    }

    /**
     * Checks if the provided menu index should be visible or hidden compared to another index.
     *
     * @param index     the index to check
     * @param indexHide the index to hide
     * @return true if the indices do not match, false if they are exactly the same
     */
    private static boolean checkMenu(int[] index, int[] indexHide) {
        if (index.length == indexHide.length) {
            for (int i = 0; i < index.length; i++) {
                if (index[i] != indexHide[i]) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Validates menu access for a given form class.
     *
     * @param itemClass the class type of the form (extends {@link Form})
     * @return true if the menu is accessible, false otherwise
     */
    public static boolean validation(Class<? extends Form> itemClass) {
        int[] index = Drawer.getMenuIndexClass(itemClass);
        if (index == null) {
            return false;
        }
        return validation(index);
    }

    /**
     * Validates menu access for the provided index according to user role.
     * Admin is allowed all. Other roles can be restricted by uncommenting sample logic.
     *
     * @param index the menu index
     * @return true if the menu is accessible, false otherwise
     */
    public static boolean validation(int[] index) {
        if (user == null) {
            return false;
        }
        if (user.getRole() == ModelUser.Role.ADMIN) {
            return true;
        }
        // Uncomment below to enable more fine-grained access for non-admin users.
        /*
        boolean status
                = checkMenu(index, new int[]{2})
                &&
                checkMenu(index, new int[]{2, 0})
                // `Components`->`Toast`
                && checkMenu(index, new int[]{2, 1})
                // `Forms`->`Responsive Layout`
                && checkMenu(index, new int[]{1, 2});

        return status;
        */
        return true;
    }
}
