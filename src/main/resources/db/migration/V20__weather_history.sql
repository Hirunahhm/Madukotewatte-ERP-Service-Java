-- Daily weather history for the estate (Open-Meteo), used for insights and tapping prediction.
CREATE TABLE weather_daily (
    weather_date                    DATE            PRIMARY KEY,
    precipitation_mm                NUMERIC(8, 2),
    rain_hours                      NUMERIC(5, 1),
    morning_rain_mm                 NUMERIC(8, 2),   -- sum of hourly precipitation 04:00-09:00 local (tapping window)
    morning_rain_probability_max    NUMERIC(5, 1),   -- only available for forecast-sourced rows
    temp_max                        NUMERIC(5, 1),
    temp_min                        NUMERIC(5, 1),
    humidity_mean                   NUMERIC(5, 1),
    wind_max                        NUMERIC(6, 1),
    et0_mm                          NUMERIC(6, 2),
    source                          VARCHAR(10)     NOT NULL CHECK (source IN ('archive', 'forecast')),
    fetched_at                      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at                      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at                      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);
