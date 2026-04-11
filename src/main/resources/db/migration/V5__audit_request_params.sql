ALTER TABLE sys_oper_log
ADD COLUMN request_params TEXT NULL COMMENT '请求参数(脱敏)';
