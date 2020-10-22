package net.maxsmr.core_network.model.request;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.maxsmr.core_network.model.request.api.async.IAsyncApiRequest;

import org.json.JSONException;
import org.json.JSONObject;

import static net.maxsmr.core_network.model.request.api.async.IAsyncApiRequestKt.DEFAULT_MAX_COUNTER;

/**
 * Содержит минимальную информацию для получения данных в асинхронных запросах
 *
 * @see IAsyncApiRequest
 */
public class SimpleRequestInfo {

    public static final String RID_TAG = "rid";

    /**
     * версия метода
     */
    public final String version;

    /**
     * имя метода в формате контроллер/метод
     */
    public final String method;

    /**
     * уникальный id запроса на основе его параметров
     */
    public final String requestsId;

    /**
     * интервал между попытками запросов
     */
    public final long updateInterval;

    /**
     * требуется ли показ ошибок
     */
    public final boolean requireError;

    public final String tag;

    /**
     * использовать только русскую локаль (RZD-5661)
     */
    public final boolean useOnlyRussianLocale;

    /**
     * Исходный асинхронный запрос, для которого требуется GetResult
     */
    public final IAsyncApiRequest originalAsyncApiRequest;

    /**
     * тело JSON из исходного запроса
     */
    @Nullable
    private final JSONObject originalRequestBody;

    public final int maxCounter;

    /**
     * Содержит промежуточный rid и, если был получен, исходный запрос
     */
    @NonNull
    private JSONObject ridRequestBody = new JSONObject();

    @Nullable
    private String rid;

    /**
     * счетчик выполненных запросов с полученным rid<p>
     * если счетчик превысил максимальное значение {@link SimpleRequestInfo#maxCounter}, считается что сервер отвалился по таймауту
     *
     */
    private int counter;

    /**
     * Копирующий конструктор при повторных запросах с rid. Обновляет поле {@link SimpleRequestInfo#counter}
     */
    public SimpleRequestInfo(SimpleRequestInfo info, @NonNull String rid, int counter) {
        this.counter = counter;
        this.version = info.version;
        this.method = info.method;
        this.updateInterval = info.updateInterval;
        this.requestsId = info.requestsId;
        this.requireError = info.requireError;
        this.maxCounter = info.maxCounter;
        this.tag = info.tag;
        this.useOnlyRussianLocale = info.useOnlyRussianLocale;
        this.originalRequestBody = info.originalRequestBody;
        this.originalAsyncApiRequest = info.originalAsyncApiRequest;
        // rid не берём из исходного, т.к. он может отличаться от актуального в параметрах
        applyRid(rid);
    }

    public SimpleRequestInfo(IAsyncApiRequest request) {
        this.version = request.getVersion();
        this.method = request.getMethod();
        this.updateInterval = request.getUpdateInterval();
        this.requestsId = request.getRequestId();
        this.requireError = request.isRequireDisplayErrorMessage();
        this.maxCounter = request.getMaxRequestCount();
        this.tag = request.getTag();
        this.useOnlyRussianLocale = request.useOnlyRussianLocale();
        this.originalRequestBody = request.getOriginalJSONBodyRequest();
        this.originalAsyncApiRequest = request;
        applyRid(null);
    }

    /**
     * Конструктор для передачи rid в последующие запросы
     *
     * @param request исходный запрос
     */
    public SimpleRequestInfo(IAsyncApiRequest request, @NonNull String rid, int counter) {
        this(request);
        this.counter = counter;
        applyRid(rid);
    }

    @Nullable
    public String getRid() {
        return rid;
    }

    public int getCounter() {
        return counter;
    }

    @Nullable
    public JSONObject getOriginalRequestBody() {
        return originalRequestBody;
    }

    @NonNull
    public JSONObject getRidRequestBody() {
        return ridRequestBody;
    }

    @Override
    @NonNull
    public String toString() {
        return String.format("%s_%s", method, requestsId);
    }

    private void applyRid(@Nullable String rid) {
        this.rid = rid;
        try {
            // из исходного, запомненного ранее, делаем копию
            // и добавляем "rid"
            ridRequestBody = originalRequestBody != null? new JSONObject(originalRequestBody.toString()) : new JSONObject();
            ridRequestBody.put(RID_TAG, rid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}