import { App } from 'antd';
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import api from '../../api/axiosConfig';
import { useCookies } from 'react-cookie';
import { Button, Form, Input, Space } from 'antd';

function CloneGit({ setTrigger }) {
	const [form] = Form.useForm();
	const [loading, setloading] = useState(false);
	const [cookies] = useCookies(['user']);
	const { notification, message } = App.useApp();

	const showNotification = (status, message) => {
		notification.info({
			message: `${status}`,
			description: `${cookies.user.myUser.userName}  ${message}`,
			placement: 'bottomLeft'
		});
	};

	const handleFetchData = async (url, commitId) => {
		setloading(true);
		try {
			const response = await api.post('/api/fetch-repo', new URLSearchParams({ url, commitId }));

			const { status, message } = response.data;

			console.log(status);
			if (status === 'CLONE_SUCCESS' || status === 'PULL_SUCCESS') {
				showNotification(status);
				form.resetFields();

				if (setTrigger) {
					setTrigger();
				}
			} else if (status === 'ANALYSIS_FAILED') {
				showNotification(message);
			} else if (status === 'PULL_FAILED' || status === 'CLONE_FAILED') {
				showNotification(status);
			} else {
				showNotification(status);
			}
		} catch (error) {
			showNotification('FetchError', error);
			console.error('Error during fetch:', error);
		} finally {
			setloading(false);
		}
	};

	const handleClick = (values) => {
		console.log(values.url, values.commitId);
		if (values.url.trim()) {
			console.log('Submitting URL:', values.url);
			if (values.commitId === undefined) {
				handleFetchData(values.url, 'HEAD');
				console.log('Submitting CommitID: ', values.commitId);
			} else {
				console.log('Submitting CommitID: ', values.commitId);
				handleFetchData(values.url, values.commitId);
			}
		} else {
			message.error('URL is empty');
		}
	};

	return (
		<div>
			<Form
				form={form}
				onFinish={handleClick}
				name="wrap"
				labelCol={{
					flex: '110px'
				}}
				labelAlign="left"
				labelWrap
				wrapperCol={{
					flex: 1
				}}
				colon={false}
				style={{
					maxWidth: 600
				}}>
				{' '}
				<Form.Item
					name="url"
					label="URL"
					rules={[
						{
							required: true
						},
						{
							type: 'url',
							warningOnly: true
						},
						{
							type: 'string',
							min: 6
						}
					]}>
					<Input placeholder="GitHub repository url" />
				</Form.Item>
				<Form.Item
					name="commitId"
					label="COMMITID"
					rules={[
						{
							required: false
						},
						{
							type: 'string',
							min: 6
						}
					]}>
					<Input placeholder="GitHub commitId" />
				</Form.Item>
				<Form.Item>
					<Space>
						<Button htmlType="submit" loading={loading}>
							Submit
						</Button>
					</Space>
				</Form.Item>
			</Form>
		</div>
	);
}

CloneGit.propTypes = {
	setTrigger: PropTypes.func.isRequired
};

export default CloneGit;
