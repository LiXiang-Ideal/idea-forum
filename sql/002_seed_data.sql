USE dev_community;

INSERT INTO categories (name, slug, description, sort_order) VALUES
  ('前端开发', 'frontend', '浏览器、框架与用户界面工程', 10),
  ('后端开发', 'backend', '服务端架构、API 与工程实践', 20),
  ('人工智能', 'artificial-intelligence', '机器学习与生成式 AI', 30),
  ('开发工具', 'developer-tools', '提升开发效率的工具与工作流', 40),
  ('阅读', 'reading', '技术阅读与经验分享', 50)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  sort_order = VALUES(sort_order);

INSERT INTO tags (name, slug) VALUES
  ('Vue', 'vue'),
  ('Java', 'java'),
  ('工程实践', 'engineering'),
  ('性能优化', 'performance'),
  ('开源', 'open-source')
ON DUPLICATE KEY UPDATE name = VALUES(name);
