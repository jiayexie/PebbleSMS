#include "pebble_os.h"
#include "pebble_app.h"
#include "pebble_fonts.h"


#define MY_UUID { 0x95, 0x7F, 0x93, 0x78, 0xC8, 0x03, 0x40, 0x0D, 0x83, 0x2F, 0x4A, 0xC5, 0xC3, 0xFD, 0x65, 0xAE }
PBL_APP_INFO(MY_UUID,
             "Pebble SMS", "Jiaye Xie",
             1, 0, /* App version */
             DEFAULT_MENU_ICON,
             APP_INFO_STANDARD_APP);

Window window;
MenuLayer menu_layer;

enum {
  KEY_FETCH_LIST,
  KEY_SEND,
  KEY_VIB,
  KEY_ENTRY_CNT,
  KEY_ENTRIES,
};

#define MAX_TITLE_LENGTH (20)
#define MAX_ENTRY_ITEMS (20)

typedef struct {
  char titles[MAX_ENTRY_ITEMS][MAX_TITLE_LENGTH];
  uint8_t entry_cnt;
} EntryList;

EntryList entry_list_data;

static void send_cmd(uint8_t cmd) {
  DictionaryIterator *iter;
  app_message_out_get(&iter);

  if (iter == NULL) {
    return;
  }

  dict_write_uint8(iter, KEY_SEND, cmd);
  dict_write_end(iter);

  app_message_out_send();
  app_message_out_release();
}

void fetch_entry_list() {
  DictionaryIterator *iter;
  app_message_out_get(&iter);

  if (iter == NULL) { 
    return;
  }

  dict_write_uint8(iter, KEY_FETCH_LIST, 0);
  dict_write_end(iter);

  app_message_out_send();
  app_message_out_release();
}

void in_received_handler(DictionaryIterator *iter, void *context) {
  Tuple *vib = dict_find(iter, KEY_VIB);
  if (vib) {
    vibes_short_pulse();
  }
  
  Tuple *count = dict_find(iter, KEY_ENTRY_CNT);
  if (count) {
    entry_list_data.entry_cnt = count->value->uint8;
    Tuple *titles = dict_find(iter, KEY_ENTRIES);
    char tmp[MAX_ENTRY_ITEMS * MAX_TITLE_LENGTH];
    strcpy(tmp, titles->value->cstring);
    // parse array
    char *p = tmp;
    int index = 0;
    int entry_len = 0;
    int cnt = entry_list_data.entry_cnt;
    while (cnt--) {
      while (*p != '\n') {
        entry_list_data.titles[index][entry_len++] = *p;
        p++;
      }
      entry_list_data.titles[index][entry_len] = '\0';
      entry_len = 0;
      index++;
      p++;
    }
    menu_layer_reload_data(&menu_layer);
  }
}

/*void select_single_click_handler(ClickRecognizerRef recognizer, Window *window) {
  send_cmd(1);
}

void click_config_provider(ClickConfig **config, Window *window) {
  config[BUTTON_ID_SELECT]->click.handler = (ClickHandler) select_single_click_handler;
  config[BUTTON_ID_SELECT]->click.repeat_interval_ms = 100;
}*/

int16_t get_cell_height_callback(struct MenuLayer *menu_layer, MenuIndex *cell_index, void *data) {
  return 44;
}

void draw_row_callback(GContext *ctx, Layer *cell_layer, MenuIndex *cell_index, void *data) {
  menu_cell_basic_draw(ctx, cell_layer, entry_list_data.titles[cell_index->row], NULL, NULL);
}

uint16_t get_num_rows_callback(struct MenuLayer *menu_layer, uint16_t section_index, void *data) {
  return entry_list_data.entry_cnt;
}

void select_callback(MenuLayer *menu_layer, MenuIndex *cell_index, void *data) {
  vibes_short_pulse();
  send_cmd(cell_index->row);
}

/*void sync_tuple_changed_callback(const uint32_t key, const Tuple* new_tuple, const Tuple *old_tuple, void *context) {
  vibes_short_pulse();
  switch (key) {
  case KEY_ENTRY_CNT:
    entry_list_data.entry_cnt = new_tuple->value->uint8;
    for (int i = 0; i < entry_list_data.entry_cnt; i++) {
      vibes_short_pulse();
    }
    break;
  case KEY_ENTRIES:
    strcpy(entry_list_data.titles, new_tuple->value->cstring);
    break;
  }
  menu_layer_reload_data(&menu_layer);
}

void sync_error_callback(DictionaryResult dict_error, AppMessageResult app_message_error, void *context) {
  // TODO error handling
}*/

void window_load(Window *window) {
  fetch_entry_list();

  app_comm_set_sniff_interval(SNIFF_INTERVAL_REDUCED);

  menu_layer_init(&menu_layer, window->layer.bounds);
  menu_layer_set_callbacks(&menu_layer, NULL, (MenuLayerCallbacks) {
    .get_cell_height = (MenuLayerGetCellHeightCallback) get_cell_height_callback,
    .draw_row = (MenuLayerDrawRowCallback) draw_row_callback,
    .get_num_rows = (MenuLayerGetNumberOfRowsInSectionsCallback) get_num_rows_callback,
    .select_click = (MenuLayerSelectCallback) select_callback,
    //.select_long_click = (MenuLayerSelectCallback) select_long_callback
  });
  menu_layer_set_click_config_onto_window(&menu_layer, window);
  layer_add_child(&window->layer, menu_layer_get_layer(&menu_layer));
}

void handle_init(AppContextRef ctx) {
  window_init(&window, "Window Name");
  window_set_window_handlers(&window, (WindowHandlers) {
    .load = window_load,
  });
  window_stack_push(&window, true /* Animated */);
}

void pbl_main(void *params) {
  PebbleAppHandlers handlers = {
    .init_handler = &handle_init,
    .messaging_info = {
      .buffer_sizes = {
        .inbound = 256,
        .outbound = 128,
      },
      .default_callbacks.callbacks = {
        .in_received = in_received_handler,
      },
    },
  };
  app_event_loop(params, &handlers);
}
