create table profile(
  _id integer primary key,
  photo text,
  photo_big text,
  first_name text,
  last_name text,
  friend integer, -- друг(1), послан запрос(2), принят запрос(3), suggestion(4)
  online integer,
  search integer,
  bdate integer,
  sex integer,
  phone text,
  contact integer,
  phone_name text,
  search_string text,
  pop integer
);

create table dialog(
  _id integer primary key autoincrement,
  chat_id integer,
  user_id integer,
  user_ids text,
  title text,
  last_message_id integer,
  search integer,
  regular integer
);

create table message (
  _id integer primary key,
  dialog_id integer references dialog(_id),
  writer_id integer references profile(_id),
  dt integer,
  body text,
  local_status integer default 0, -- локальный статус = прочитано или нет
  server_status integer default 0, -- серверный статус = прочитано или нет (для исходящих сообщений такое же значение, как и local_status)
  attachment text,
  deleted integer,
  search integer,
  regular integer
);

create index if not exists IDX_MESSAGE_DIALOG on message(dialog_id);
create index if not exists IDX_MESSAGE_LOCAL on message(local_status);
create index if not exists IDX_MESSAGE_SERVER on message(server_status);

create view dialog_v as
select dialog._id _id,
       dialog.user_id user_id,
       dialog.chat_id chat_id,
       dialog.title title,
       dialog.last_message_id last_message_id,
       dialog.user_ids user_ids,
       dialog.regular regular,
       dialog.search search,
       message._id message_id,
       message.writer_id writer_id,
       message.body body,
       message.dt dt,
       message.local_status local_status,
       message.server_status server_status,
       profile.first_name first_name,
       profile.last_name last_name,
       profile.photo photo,
       profile.online online,
       writer.first_name writer_first_name,
       writer.last_name writer_last_name
  from dialog
  left join profile on dialog.user_id = profile._id
  left join message on message._id = dialog.last_message_id
  left join profile writer on writer._id = message.writer_id;

create view message_v as
select message._id _id,
       message.dialog_id dialog_id,
       message.writer_id writer_id,
       message.body body,
       message.dt dt,
       message.local_status local_status,
       message.server_status server_status,
       message.attachment attachment,
       message.deleted deleted,
       message.search search,
       message.regular regular,
       profile.first_name first_name,
       profile.last_name last_name,
       profile.photo photo,
       profile.online online
  from message
  left join profile on message.writer_id = profile._id;

create view message_dialog_v as
select message._id _id,
       message._id message_id,
       message.dialog_id dialog_id,
       message.writer_id writer_id,
       message.body body,
       message.dt dt,
       message.local_status local_status,
       message.server_status server_status,
       message.attachment attachment,
       message.deleted deleted,
       message.search search,
       message.regular regular,
       profile.first_name first_name,
       profile.last_name last_name,
       profile.photo photo,
       dialog.title title,
       dialog.user_id user_id,
       dialog.chat_id chat_id,
       profile.online online
  from message
  left join profile on message.writer_id = profile._id
  left join dialog on dialog._id = message.dialog_id;