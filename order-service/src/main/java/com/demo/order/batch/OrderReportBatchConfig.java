package com.demo.order.batch;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.transaction.PlatformTransactionManager;

import com.demo.order.entity.Order;

/**
 * Spring Batch configuration for generating daily order reports.
 * 
 * <p>
 * Reads completed orders from the database, processes them, and writes a
 * summary report to a CSV file.
 */
@Configuration
public class OrderReportBatchConfig {

	/**
	 * Reader: Fetches orders that are DELIVERED.
	 */
	@Bean
	public JdbcCursorItemReader<Order> orderItemReader(DataSource dataSource) {
		return new JdbcCursorItemReaderBuilder<Order>().name("orderItemReader").dataSource(dataSource)
				.sql("SELECT * FROM orders WHERE status = 'DELIVERED'")
				.rowMapper(new BeanPropertyRowMapper<>(Order.class)).build();
	}

	/**
	 * Processor: Transforms Order into a CSV string format.
	 */
	@Bean
	public ItemProcessor<Order, String> orderItemProcessor() {
		return order -> String.format("%s,%s,%s,%s,%s", order.getId(), order.getUserId(), order.getProductId(),
				order.getTotalPrice(), order.getCreatedAt());
	}

	/**
	 * Writer: Writes the processed lines to a CSV file.
	 */
	@Bean
	public FlatFileItemWriter<String> orderItemWriter() {
		return new FlatFileItemWriterBuilder<String>().name("orderItemWriter")
				.resource(new FileSystemResource("target/order-report.csv")).lineAggregator(item -> item)
				.headerCallback(writer -> writer.write("ORDER_ID,USER_ID,PRODUCT_ID,TOTAL_PRICE,CREATED_AT")).build();
	}

	@Bean
	public Step generateReportStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			JdbcCursorItemReader<Order> reader, ItemProcessor<Order, String> processor,
			FlatFileItemWriter<String> writer) {
		return new StepBuilder("generateReportStep", jobRepository).<Order, String>chunk(100, transactionManager)
				.reader(reader).processor(processor).writer(writer).build();
	}

	@Bean
	public Job orderReportJob(JobRepository jobRepository, Step generateReportStep) {
		return new JobBuilder("orderReportJob", jobRepository).start(generateReportStep).build();
	}
}
