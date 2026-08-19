/**
 * FXML controllers - one per view, named {@code <View>Controller}.
 *
 * Rule: a controller binds nodes, validates input and calls exactly one
 * service. It never imports a DAO, never opens a Connection, and never
 * contains money arithmetic.
 *
 * It also may not decide permissions. It only reflects them - hiding a button
 * is presentation. The service is what refuses the call. See defect D03: the
 * legacy app's permission check was a client-side redirect that ran after the
 * page had already queried and sent the data.
 */
package lk.com.synsoft.offlinepos.controller;
